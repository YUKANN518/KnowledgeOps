param([string]$BaseUrl = 'http://localhost:5173/api/v1')

$ErrorActionPreference = 'Stop'
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$settings = @{}
Get-Content (Join-Path $projectRoot '.env') | Where-Object { $_ -match '^[A-Z0-9_]+=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  $settings[$name] = $value
}

function Assert-Equal($actual, $expected, [string]$label) {
  if ($actual -ne $expected) { throw "$label expected '$expected' but received '$actual'" }
}
function Headers([string]$token) { return @{ Authorization = "Bearer $token" } }
function Login([string]$email, [string]$password, [Microsoft.PowerShell.Commands.WebRequestSession]$session) {
  $body = @{ email = $email; password = $password } | ConvertTo-Json
  return Invoke-RestMethod "$BaseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $body -WebSession $session
}
function Create-User($adminToken, $email, $name, $password, $role) {
  $body = @{ email = $email; displayName = $name; initialPassword = $password; roles = @($role) } | ConvertTo-Json
  return Invoke-RestMethod "$BaseUrl/users" -Method Post -Headers (Headers $adminToken) -ContentType 'application/json' -Body $body
}
function Expect-Forbidden([scriptblock]$request, [string]$label) {
  try { & $request; throw "$label unexpectedly succeeded" }
  catch {
    $status = [int]$_.Exception.Response.StatusCode
    if ($status -ne 403) { throw "$label expected 403 but received $status" }
  }
}
function Upload-Document($token, $categoryId, $path, $contentType) {
  $response = & curl.exe -fsS -X POST -H "Authorization: Bearer $token" -F "categoryId=$categoryId" -F "file=@$path;type=$contentType" "$BaseUrl/documents"
  if ($LASTEXITCODE -ne 0) { throw "document upload failed for $path" }
  return $response | ConvertFrom-Json
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$adminSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$admin = Login $settings.BOOTSTRAP_ADMIN_EMAIL $settings.BOOTSTRAP_ADMIN_PASSWORD $adminSession
$adminHeaders = Headers $admin.accessToken

$employeeEmail = "phase4.employee.$suffix@example.local"
$supportEmail = "phase4.support.$suffix@example.local"
$managerEmail = "phase4.manager.$suffix@example.local"
$employee = Create-User $admin.accessToken $employeeEmail 'Phase Four Employee' 'Phase4Employee!123' 'EMPLOYEE'
$support = Create-User $admin.accessToken $supportEmail 'Phase Four Support' 'Phase4Support!123' 'SUPPORT_AGENT'
$manager = Create-User $admin.accessToken $managerEmail 'Phase Four Manager' 'Phase4Manager!123' 'KNOWLEDGE_MANAGER'

$employeeSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$employeeLogin = Login $employeeEmail 'Phase4Employee!123' $employeeSession
$employeeHeaders = Headers $employeeLogin.accessToken
$supportSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$supportLogin = Login $supportEmail 'Phase4Support!123' $supportSession
$supportHeaders = Headers $supportLogin.accessToken
$managerSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$managerLogin = Login $managerEmail 'Phase4Manager!123' $managerSession
$managerHeaders = Headers $managerLogin.accessToken

$me = Invoke-RestMethod "$BaseUrl/users/me" -Headers $employeeHeaders
Assert-Equal $me.email $employeeEmail 'current user'

$ticketBody = @{ title = "Phase 4 browser proxy smoke $suffix"; description = 'Created through the frontend reverse proxy.'; priority = 'HIGH' } | ConvertTo-Json
$ticket = Invoke-RestMethod "$BaseUrl/tickets" -Method Post -Headers $employeeHeaders -ContentType 'application/json' -Body $ticketBody
$commentBody = @{ content = 'Employee supplied a reproducible update.'; expectedVersion = $ticket.version } | ConvertTo-Json
$null = Invoke-RestMethod "$BaseUrl/tickets/$($ticket.id)/comments" -Method Post -Headers $employeeHeaders -ContentType 'application/json' -Body $commentBody
$ticket = Invoke-RestMethod "$BaseUrl/tickets/$($ticket.id)" -Headers $supportHeaders
$assignmentBody = @{ assigneeId = $support.id; expectedVersion = $ticket.version } | ConvertTo-Json
$ticket = Invoke-RestMethod "$BaseUrl/tickets/$($ticket.id)/assignments" -Method Post -Headers $supportHeaders -ContentType 'application/json' -Body $assignmentBody
$transitionBody = @{ status = 'IN_PROGRESS'; expectedVersion = $ticket.version } | ConvertTo-Json
$ticket = Invoke-RestMethod "$BaseUrl/tickets/$($ticket.id)/transitions" -Method Post -Headers $supportHeaders -ContentType 'application/json' -Body $transitionBody
Assert-Equal $ticket.status 'IN_PROGRESS' 'ticket transition'
$ticketList = Invoke-RestMethod "$BaseUrl/tickets?page=0&size=10" -Headers $employeeHeaders
if (-not ($ticketList.content.id -contains $ticket.id)) { throw 'created ticket is missing from employee list' }

$categoryBody = @{ name = "Phase 4 Operations $suffix"; description = 'Runtime verification category' } | ConvertTo-Json
$category = Invoke-RestMethod "$BaseUrl/knowledge/categories" -Method Post -Headers $managerHeaders -ContentType 'application/json' -Body $categoryBody
Expect-Forbidden { Invoke-RestMethod "$BaseUrl/knowledge/categories" -Method Post -Headers $employeeHeaders -ContentType 'application/json' -Body (@{name='Forbidden category'} | ConvertTo-Json) } 'employee category creation'

$articleBody = @{ title = "Phase 4 runbook $suffix"; content = 'Initial runtime instructions.'; categoryId = $category.id } | ConvertTo-Json
$article = Invoke-RestMethod "$BaseUrl/knowledge/articles" -Method Post -Headers $managerHeaders -ContentType 'application/json' -Body $articleBody
$updateBody = @{ title = "Phase 4 verified runbook $suffix"; content = 'Approved runtime instructions.'; categoryId = $category.id; expectedVersion = $article.version } | ConvertTo-Json
$article = Invoke-RestMethod "$BaseUrl/knowledge/articles/$($article.id)" -Method Put -Headers $managerHeaders -ContentType 'application/json' -Body $updateBody
$publishBody = @{ expectedVersion = $article.version } | ConvertTo-Json
$article = Invoke-RestMethod "$BaseUrl/knowledge/articles/$($article.id)/publish" -Method Post -Headers $managerHeaders -ContentType 'application/json' -Body $publishBody
$employeeArticle = Invoke-RestMethod "$BaseUrl/knowledge/articles/$($article.id)" -Headers $employeeHeaders
Assert-Equal $employeeArticle.status 'PUBLISHED' 'employee article read'

$tempDirectory = Join-Path ([IO.Path]::GetTempPath()) "knowledgeops-phase4-$suffix"
$null = New-Item $tempDirectory -ItemType Directory
$textFile = Join-Path $tempDirectory 'phase4-smoke.txt'
$pdfFile = Join-Path $tempDirectory 'phase4-smoke.pdf'
[IO.File]::WriteAllText($textFile, 'KnowledgeOps Phase 4 document smoke')
[IO.File]::WriteAllText($pdfFile, "%PDF-1.4`nKnowledgeOps Phase 4")
$textDocument = Upload-Document $managerLogin.accessToken $category.id $textFile 'text/plain'
$pdfDocument = Upload-Document $managerLogin.accessToken $category.id $pdfFile 'application/pdf'
$documentList = Invoke-RestMethod "$BaseUrl/documents?page=0&size=20" -Headers $employeeHeaders
if (-not ($documentList.content.id -contains $textDocument.id)) { throw 'uploaded document is missing from employee list' }
$downloadPath = Join-Path $tempDirectory 'downloaded.txt'
$download = Invoke-WebRequest "$BaseUrl/documents/$($textDocument.id)/content" -Headers $employeeHeaders -OutFile $downloadPath -PassThru
Assert-Equal $download.StatusCode 200 'document download'
Assert-Equal ([IO.File]::ReadAllText($downloadPath)) 'KnowledgeOps Phase 4 document smoke' 'download content'
$archived = Invoke-RestMethod "$BaseUrl/documents/$($pdfDocument.id)/archive" -Method Post -Headers $managerHeaders
Assert-Equal $archived.status 'ARCHIVED' 'document archive'

$refreshHeaders = @{ 'X-CSRF-Token' = $employeeLogin.csrfToken; Origin = 'http://localhost:5173' }
$refreshed = Invoke-RestMethod "$BaseUrl/auth/refresh" -Method Post -Headers $refreshHeaders -WebSession $employeeSession
$restoredMe = Invoke-RestMethod "$BaseUrl/users/me" -Headers (Headers $refreshed.accessToken)
Assert-Equal $restoredMe.email $employeeEmail 'session restore'
$logoutHeaders = @{ 'X-CSRF-Token' = $refreshed.csrfToken; Origin = 'http://localhost:5173' }
Invoke-RestMethod "$BaseUrl/auth/logout" -Method Post -Headers $logoutHeaders -WebSession $employeeSession
try { Invoke-RestMethod "$BaseUrl/auth/refresh" -Method Post -Headers $logoutHeaders -WebSession $employeeSession; throw 'refresh after logout unexpectedly succeeded' }
catch {
  $status = [int]$_.Exception.Response.StatusCode
  if ($status -notin @(401, 403)) { throw "refresh after logout expected rejection but received $status" }
}

Remove-Item $tempDirectory -Recurse -Force
[ordered]@{
  authentication = 'PASS'; ticket = 'PASS'; knowledge = 'PASS'; documents = 'PASS'; rbac403 = 'PASS'
  ticketId = $ticket.id; articleId = $article.id; employeeEmail = $employeeEmail; managerEmail = $managerEmail; supportEmail = $supportEmail; supportId = $support.id
} | ConvertTo-Json
