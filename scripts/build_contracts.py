"""Architecture-only contract authoring. No application endpoints are implemented."""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs' / 'contracts'


def obj(properties, required=None):
    return {'type': 'object', 'additionalProperties': False, 'properties': properties,
            'required': list(properties) if required is None else required}


def string(maximum=200, **kw):
    return {'type': 'string', 'minLength': 1, 'maxLength': maximum, **kw}


def enum(*values):
    return {'type': 'string', 'enum': list(values)}


def arr(items, maximum=100):
    return {'type': 'array', 'items': items, 'maxItems': maximum}


def ref(name):
    return {'$ref': '#/components/schemas/' + name}


def nullable(schema):
    return {'anyOf': [schema, {'type': 'null'}]}


ID = {'type': 'string', 'format': 'uuid'}
TIME = {'type': 'string', 'format': 'date-time'}
INT = {'type': 'integer', 'minimum': 0}
HASH = {'type': 'string', 'pattern': '^[0-9a-f]{64}$'}
STATUS = enum('OPEN', 'IN_PROGRESS', 'WAITING_USER', 'RESOLVED', 'CLOSED', 'CANCELLED')
PRIORITY = enum('LOW', 'NORMAL', 'HIGH', 'URGENT')
CATEGORY = enum('IT', 'HR', 'FACILITIES', 'OTHER')
ROLE = enum('EMPLOYEE', 'SUPPORT_AGENT', 'KNOWLEDGE_MANAGER', 'ADMINISTRATOR')
tool_args = {
    'searchKnowledge': obj({'query': string(2000), 'knowledgeBaseIds': arr(ID, 100)}, ['query']),
    'getTicket': obj({'ticketId': ID}),
    'listTickets': obj({'status': STATUS, 'page': {**INT, 'maximum': 1000},
                       'size': {'type': 'integer', 'minimum': 1, 'maximum': 100}}, []),
    'createTicketDraft': obj({'title': string(200), 'description': string(10000),
                              'category': CATEGORY, 'priority': PRIORITY}),
    'assignTicketDraft': obj({'ticketId': ID, 'assigneeId': ID, 'reason': string(1000)},
                            ['ticketId', 'assigneeId']),
    'updateTicketStatusDraft': obj({'ticketId': ID, 'targetStatus': STATUS, 'reason': string(2000)},
                                  ['ticketId', 'targetStatus']),
    'addTicketCommentDraft': obj({'ticketId': ID, 'body': string(5000),
                                  'visibility': enum('PUBLIC', 'INTERNAL')}),
}
tools_doc = {'$schema': 'https://json-schema.org/draft/2020-12/schema',
             '$id': 'https://knowledgeops.example/schemas/tool-call.json',
             'title': 'KnowledgeOps strict tool call',
             '$defs': tool_args,
             'oneOf': [obj({'name': {'const': name}, 'arguments': {'$ref': '#/$defs/' + name}})
                       for name in tool_args]}
S = {}
S['Error'] = obj({'code': string(80), 'message': string(500),
                  'details': arr(obj({'field': string(100), 'reason': string(300)}), 30),
                  'requestId': string(64), 'timestamp': TIME})
S['IdResult'] = obj({'id': ID})
S['Health'] = obj({'status': enum('UP', 'DEGRADED', 'DOWN'), 'version': string(80)})
S['Department'] = obj({'id': ID, 'code': string(40), 'name': string(100), 'active': {'type': 'boolean'}})
S['DepartmentCreate'] = obj({'code': string(40), 'name': string(100)})
S['DepartmentUpdate'] = obj({'name': string(100), 'active': {'type': 'boolean'}})
S['User'] = obj({'id': ID, 'username': string(64), 'displayName': string(100), 'departmentId': ID,
                 'roles': arr(ROLE, 4), 'status': enum('ACTIVE', 'DISABLED'), 'version': INT})
S['UserCreate'] = obj({'username': string(64), 'displayName': string(100), 'departmentId': ID,
                       'roles': arr(ROLE, 4), 'initialPassword': string(72, minLength=12)})
S['UserUpdate'] = obj({'displayName': string(100), 'departmentId': ID, 'roles': arr(ROLE, 4),
                       'status': enum('ACTIVE', 'DISABLED'), 'expectedVersion': INT})
S['Role'] = obj({'code': ROLE, 'permissions': arr(string(80))})
S['Login'] = obj({'username': string(64), 'password': string(72)})
S['Tokens'] = obj({'accessToken': string(4096), 'expiresIn': {'const': 600},
                   'csrfToken': string(256), 'user': ref('User')})
S['KnowledgeBase'] = obj({'id': ID, 'name': string(120), 'description': {'type': 'string', 'maxLength': 1000},
                          'status': enum('ACTIVE', 'ARCHIVED'), 'version': INT})
S['KnowledgeBaseCreate'] = obj({'name': string(120), 'description': {'type': 'string', 'maxLength': 1000}})
S['KnowledgeBaseUpdate'] = obj({'name': string(120), 'description': {'type': 'string', 'maxLength': 1000},
                                'status': enum('ACTIVE', 'ARCHIVED'), 'expectedVersion': INT})
S['Member'] = obj({'userId': ID, 'accessLevel': enum('READER', 'MANAGER')})
S['MemberInput'] = obj({'accessLevel': enum('READER', 'MANAGER')})
S['Version'] = obj({'id': ID, 'documentId': ID, 'versionNo': {'type': 'integer', 'minimum': 1},
                    'status': enum('UPLOADED', 'PROCESSING', 'READY', 'FAILED'),
                    'sourceSha256': HASH, 'mediaType': string(100), 'createdAt': TIME,
                    'errorCode': nullable(string(80)), 'indexGeneration': nullable(ID)})
S['Document'] = obj({'id': ID, 'knowledgeBaseId': ID, 'title': string(200),
                     'status': enum('ACTIVE', 'ARCHIVED'), 'currentVersionId': nullable(ID),
                     'version': INT, 'createdAt': TIME, 'updatedAt': TIME})
S['DocumentUpload'] = obj({'knowledgeBaseId': ID, 'title': string(200),
                           'file': {'type': 'string', 'format': 'binary'}})
S['VersionUpload'] = obj({'file': {'type': 'string', 'format': 'binary'}, 'expectedVersion': INT})
S['DocumentUpdate'] = obj({'title': string(200), 'status': enum('ACTIVE', 'ARCHIVED'), 'expectedVersion': INT})
S['Job'] = obj({'id': ID, 'versionId': ID, 'status': enum('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED'),
                'step': enum('VALIDATE', 'PARSE', 'NORMALIZE', 'CHUNK', 'EMBED', 'INDEX', 'COMMIT'),
                'attempts': INT, 'errorCode': nullable(string(80)), 'updatedAt': TIME})
S['UploadAccepted'] = obj({'documentId': ID, 'versionId': ID, 'jobId': ID})
S['ExpectedVersion'] = obj({'expectedVersion': INT})
S['TicketCreate'] = copy.deepcopy(tool_args['createTicketDraft'])
S['Ticket'] = obj({'id': ID, 'ticketNumber': string(32), 'title': string(200),
                   'description': string(10000), 'category': CATEGORY, 'priority': PRIORITY,
                   'requesterId': ID, 'departmentId': ID, 'assigneeId': nullable(ID),
                   'status': STATUS, 'version': INT, 'createdAt': TIME, 'updatedAt': TIME})
S['CommentCreate'] = obj({'body': string(5000), 'visibility': enum('PUBLIC', 'INTERNAL'), 'expectedVersion': INT})
S['Comment'] = obj({'id': ID, 'ticketId': ID, 'authorId': ID, 'body': string(5000),
                    'visibility': enum('PUBLIC', 'INTERNAL'), 'origin': enum('HUMAN', 'AI_APPROVED'), 'createdAt': TIME})
S['AssignmentCreate'] = obj({'assigneeId': ID, 'reason': string(1000), 'expectedVersion': INT},
                            ['assigneeId', 'expectedVersion'])
S['Assignment'] = obj({'id': ID, 'ticketId': ID, 'fromAssigneeId': nullable(ID), 'toAssigneeId': ID,
                       'actorId': ID, 'reason': nullable(string(1000)), 'createdAt': TIME})
S['Transition'] = obj({'targetStatus': STATUS, 'reason': string(2000), 'expectedVersion': INT},
                      ['targetStatus', 'expectedVersion'])
S['StatusHistory'] = obj({'id': ID, 'fromStatus': nullable(STATUS), 'toStatus': STATUS,
                          'reason': nullable(string(2000)), 'actorId': ID, 'createdAt': TIME})
S['Conversation'] = obj({'id': ID, 'title': string(200), 'createdAt': TIME, 'updatedAt': TIME})
S['ConversationCreate'] = obj({'title': string(200)})
S['Citation'] = obj({'citationId': string(40), 'chunkId': ID, 'documentId': ID, 'versionId': ID,
                     'title': string(200), 'pageStart': nullable({'type': 'integer', 'minimum': 1}),
                     'pageEnd': nullable({'type': 'integer', 'minimum': 1}),
                     'headingPath': nullable(string(1000)), 'quote': string(3000),
                     'sourceUrl': string(500)})
S['Claim'] = obj({'text': string(4000), 'citationIds': {**arr(string(40), 8), 'minItems': 1}})
S['Search'] = copy.deepcopy(tool_args['searchKnowledge'])
S['SearchResult'] = obj({'evidence': arr(ref('Citation'), 8), 'requestId': string(64)})
S['Query'] = obj({'conversationId': ID, 'turnId': ID, 'question': string(2000),
                  'knowledgeBaseIds': arr(ID, 100)}, ['conversationId', 'turnId', 'question'])
S['QueryResult'] = obj({'messageId': ID, 'conversationId': ID, 'turnId': ID,
                        'answer': {'type': 'string', 'maxLength': 16000},
                        'outcome': enum('ANSWERED', 'INSUFFICIENT_EVIDENCE', 'INVALID_CITATION'),
                        'claims': arr(ref('Claim'), 30), 'citations': arr(ref('Citation'), 8),
                        'requestId': string(64)})
S['Message'] = obj({'id': ID, 'turnId': ID, 'role': enum('USER', 'ASSISTANT'),
                    'content': {'type': 'string', 'maxLength': 16000},
                    'outcome': string(40), 'citations': arr(ref('Citation'), 8),
                    'redacted': {'type': 'boolean'}, 'createdAt': TIME})
for name, schema in tool_args.items():
    S[name] = schema
S['ToolCall'] = {'oneOf': [obj({'name': {'const': name}, 'arguments': ref(name)}) for name in tool_args]}
S['WriteToolCall'] = {'oneOf': [obj({'name': {'const': name}, 'arguments': ref(name)})
                               for name in tool_args if name.endswith('Draft')]}
S['ToolRequest'] = obj({'conversationId': ID, 'turnId': ID, 'toolCallId': ID, 'call': ref('WriteToolCall')})
S['DraftResult'] = obj({'draftId': ID, 'status': {'const': 'PENDING'}, 'payloadHash': HASH, 'expiresAt': TIME})
S['ExecutionResult'] = obj({'resourceType': enum('TICKET', 'TICKET_COMMENT', 'TICKET_ASSIGNMENT', 'TICKET_STATUS'),
                            'resourceId': ID, 'ticketId': ID, 'ticketVersion': INT})
S['Approval'] = obj({'id': ID, 'requesterId': ID, 'conversationId': ID, 'turnId': ID,
                     'toolCallId': ID, 'call': ref('WriteToolCall'), 'payloadHash': HASH,
                     'evidenceChunkIds': arr(ID, 24),
                     'targetTicketId': nullable(ID), 'expectedVersion': nullable(INT),
                     'riskLevel': enum('LOW', 'MEDIUM', 'HIGH'),
                     'status': enum('PENDING', 'APPROVED', 'REJECTED', 'EXECUTED', 'FAILED', 'EXPIRED'),
                     'createdAt': TIME, 'expiresAt': TIME, 'approvedBy': nullable(ID),
                     'approvedAt': nullable(TIME), 'executedAt': nullable(TIME),
                     'executionResult': nullable(ref('ExecutionResult')), 'errorCode': nullable(string(80))})
S['ApprovalDecision'] = obj({'decision': enum('APPROVE', 'REJECT'), 'payloadHash': HASH,
                             'reason': string(1000)}, ['decision', 'payloadHash'])
S['AssistantResult'] = obj({'conversationId': ID, 'turnId': ID, 'answer': string(16000),
                            'outcome': enum('ANSWERED', 'DRAFT_PENDING', 'INSUFFICIENT_EVIDENCE', 'LIMIT_REACHED'),
                            'draftId': nullable(ID), 'citations': arr(ref('Citation'), 8), 'requestId': string(64)})
S['Audit'] = obj({'id': ID, 'actorId': nullable(ID), 'actorType': enum('USER', 'SERVICE', 'ANONYMOUS'),
                  'action': string(80), 'resourceType': string(64), 'resourceId': nullable(string(64)),
                  'metadata': {'type': 'object', 'description': 'Server-built allowlisted redacted metadata.'},
                  'outcome': enum('SUCCESS', 'DENIED', 'FAILURE'), 'timestamp': TIME, 'requestId': string(64)})
for name in ['Department', 'User', 'KnowledgeBase', 'Member', 'Document', 'Version', 'Ticket',
             'Comment', 'Assignment', 'StatusHistory', 'Conversation', 'Message', 'Approval', 'Audit']:
    S[name + 'Page'] = obj({'items': arr(ref(name)), 'page': INT, 'size': {'type': 'integer', 'minimum': 1, 'maximum': 100},
                            'total': INT})


def doc(title, server, security):
    return {'openapi': '3.1.0', 'info': {'title': title, 'version': '0.1.0-architecture',
             'description': 'Design contract only. Operations are not implemented.'},
            'servers': [{'url': server}], 'security': [{security: []}], 'paths': {},
            'components': {'schemas': S, 'securitySchemes': {
                'BearerAuth': {'type': 'http', 'scheme': 'bearer', 'bearerFormat': 'JWT'},
                'ServiceAuth': {'type': 'apiKey', 'in': 'header', 'name': 'X-Service-Token'},
                'RefreshCookie': {'type': 'apiKey', 'in': 'cookie', 'name': 'refresh_token'}}}}


public = doc('KnowledgeOps Java public API', '/api/v1', 'BearerAuth')
internal = doc('KnowledgeOps Java internal AI API', '/internal/ai', 'ServiceAuth')
ai = doc('KnowledgeOps Python AI service API', 'http://ai-service:8000', 'ServiceAuth')


def parameter(name, schema, location='query', required=False):
    return {'name': name, 'in': location, 'required': required, 'schema': schema}


def op(document, path, method, name, summary, request=None, response=None, status=200,
       page=False, filters=None, security=None, idem=False, multipart=False, extra=None):
    params = [parameter('X-Request-ID', string(64), 'header')]
    for part in path.split('/'):
        if part.startswith('{'):
            params.append(parameter(part[1:-1], ID, 'path', True))
    if page:
        params += [parameter('page', {**INT, 'maximum': 1000, 'default': 0}),
                   parameter('size', {'type': 'integer', 'minimum': 1, 'maximum': 100, 'default': 20}),
                   parameter('sort', enum('createdAt,desc', 'createdAt,asc'))]
    for key, value in (filters or {}).items():
        params.append(parameter(key, value))
    if idem:
        params.append(parameter('Idempotency-Key', string(100), 'header', True))
    params += extra or []
    operation = {'operationId': name, 'summary': summary, 'parameters': params,
                 'responses': {str(status): {'description': 'Successful response',
                   'headers': {'X-Request-ID': {'schema': string(64), 'description': 'Correlation identifier'}}}}}
    if response:
        schema = ref(response) if isinstance(response, str) else response
        operation['responses'][str(status)]['content'] = {'application/json': {'schema': schema}}
    for code, description in [(400, 'Malformed request'), (401, 'Authentication failed'),
                               (403, 'Forbidden operation'), (404, 'Missing or inaccessible resource'),
                               (409, 'State, version, context or idempotency conflict'),
                               (413, 'Payload too large'), (422, 'Schema or domain validation failed'),
                               (429, 'Rate limited'), (503, 'Required dependency unavailable')]:
        operation['responses'][str(code)] = {'description': description,
                  'content': {'application/json': {'schema': ref('Error')}}}
    operation['responses']['429']['headers'] = {'Retry-After': {'schema': {'type': 'integer', 'minimum': 1}}}
    if request:
        operation['requestBody'] = {'required': True, 'content': {
            'multipart/form-data' if multipart else 'application/json': {
                'schema': ref(request) if isinstance(request, str) else request}}}
    if security is not None:
        operation['security'] = security
    document['paths'].setdefault(path, {})[method.lower()] = operation


op(public, '/auth/login', 'post', 'login', 'Login; sets rotating HttpOnly refresh cookie', 'Login', 'Tokens', security=[])
csrf = [parameter('X-CSRF-Token', string(256), 'header', True), parameter('Origin', string(300), 'header', True)]
op(public, '/auth/refresh', 'post', 'refresh', 'Rotate refresh token; detect family replay', response='Tokens',
   security=[{'RefreshCookie': []}], extra=csrf)
op(public, '/auth/logout', 'post', 'logout', 'Revoke refresh family and Redis session; clear cookie', status=204,
   security=[{'RefreshCookie': []}], extra=csrf)
op(public, '/users/me', 'get', 'me', 'Current account and current permissions', response='User')
for path, name, create, update in [('/users', 'User', 'UserCreate', 'UserUpdate'),
                                   ('/departments', 'Department', 'DepartmentCreate', 'DepartmentUpdate'),
                                   ('/knowledge-bases', 'KnowledgeBase', 'KnowledgeBaseCreate', 'KnowledgeBaseUpdate')]:
    op(public, path, 'get', 'list' + name, 'List authorized ' + name, response=name + 'Page', page=True)
    op(public, path, 'post', 'create' + name, 'Administrator creates ' + name, create, name, 201, idem=True)
    op(public, path + '/{id}', 'get', 'get' + name, 'Authorized detail of ' + name, response=name)
    op(public, path + '/{id}', 'patch', 'update' + name, 'Administrator updates ' + name, update, name, idem=True)
op(public, '/roles', 'get', 'roles', 'Administrator reads fixed roles and permissions', response=arr(ref('Role'), 4))
op(public, '/knowledge-bases/{id}/members', 'get', 'listMembers', 'Administrator reads KB ACL', response='MemberPage', page=True)
op(public, '/knowledge-bases/{id}/members/{userId}', 'put', 'grantMember', 'Administrator grants explicit KB access', 'MemberInput', 'Member', idem=True)
op(public, '/knowledge-bases/{id}/members/{userId}', 'delete', 'revokeMember', 'Administrator revokes access and increments security epoch', status=204, idem=True)
op(public, '/documents', 'get', 'listDocuments', 'Only authorized active documents', response='DocumentPage', page=True,
   filters={'knowledgeBaseId': ID})
op(public, '/documents', 'post', 'uploadDocument', 'KB manager uploads immutable source; 20MiB maximum',
   'DocumentUpload', 'UploadAccepted', 202, idem=True, multipart=True)
op(public, '/documents/{id}', 'get', 'getDocument', 'Authorized document metadata', response='Document')
op(public, '/documents/{id}', 'patch', 'updateDocument', 'KB manager renames or archives document; no source replacement',
   'DocumentUpdate', 'Document', idem=True)
op(public, '/documents/{id}/versions', 'get', 'listVersions', 'Authorized version history', response='VersionPage', page=True)
op(public, '/documents/{id}/versions', 'post', 'uploadVersion', 'KB manager uploads new immutable version',
   'VersionUpload', 'UploadAccepted', 202, idem=True, multipart=True)
op(public, '/documents/{id}/versions/{versionId}/content', 'get', 'downloadVersion', 'Authorized attachment; never public static URL')
public['paths']['/documents/{id}/versions/{versionId}/content']['get']['responses']['200']['content'] = {
    'application/octet-stream': {'schema': {'type': 'string', 'format': 'binary'}}}
op(public, '/documents/{id}/versions/{versionId}/retry', 'post', 'retryDocument', 'KB manager retries FAILED version',
   response='Job', status=202, idem=True)
op(public, '/document-jobs/{id}', 'get', 'getJob', 'KB manager polls processing state', response='Job')
op(public, '/search', 'post', 'search', 'Authenticated retrieval with compulsory permission filter', 'Search', 'SearchResult')
op(public, '/ai/conversations', 'post', 'createConversation', 'Create own conversation', 'ConversationCreate', 'Conversation', 201, idem=True)
op(public, '/ai/conversations', 'get', 'listConversations', 'Only own conversations', response='ConversationPage', page=True)
op(public, '/ai/conversations/{id}/messages', 'get', 'listMessages', 'Reauthorize evidence and redact revoked answers', response='MessagePage', page=True)
op(public, '/ai/query', 'post', 'query', 'RAG answer with evidence, no tool writes', 'Query', 'QueryResult', idem=True)
op(public, '/ai/assistant', 'post', 'assistant', 'Bounded tool loop; write proposals become drafts only', 'Query', 'AssistantResult', idem=True)
op(public, '/ai/tool-requests', 'post', 'requestToolDraft', 'Only recorded pending model call; server checks ownership and exact call',
   'ToolRequest', 'DraftResult', 201, idem=True)
op(public, '/ai/approvals', 'get', 'listApprovals', 'Only own approvals', response='ApprovalPage', page=True,
   filters={'status': S['Approval']['properties']['status']})
op(public, '/ai/approvals/{id}', 'get', 'getApproval', 'Exact immutable payload and execution result', response='Approval')
op(public, '/ai/approvals/{id}/decision', 'post', 'decideApproval', 'Human owner confirms displayed payload hash or rejects',
   'ApprovalDecision', 'Approval', idem=True)
op(public, '/ai/approvals/{id}/execute', 'post', 'executeApproval', 'Java reauthorizes and atomically executes APPROVED action once',
   response='Approval', idem=True)
op(public, '/tickets', 'get', 'listTickets', 'Resource-filtered tickets; total uses identical ACL', response='TicketPage', page=True,
   filters={'status': STATUS, 'assigneeId': ID, 'priority': PRIORITY, 'category': CATEGORY})
op(public, '/tickets', 'post', 'createTicket', 'Requester and department from authenticated context', 'TicketCreate', 'Ticket', 201, idem=True)
op(public, '/tickets/{id}', 'get', 'getTicket', 'Owner, department Support, or Administrator', response='Ticket')
S['AssigneeCandidate'] = obj({'id': ID, 'displayName': string(100)})
op(public, '/tickets/{id}/assignee-candidates', 'get', 'assigneeCandidates',
   'Authorized assigner sees active same-department Support candidates', response=arr(ref('AssigneeCandidate')))
for route, noun, request, response in [('comments', 'Comment', 'CommentCreate', 'Comment'),
                                       ('assignments', 'Assignment', 'AssignmentCreate', 'Ticket')]:
    op(public, '/tickets/{id}/' + route, 'get', 'list' + noun, 'Authorized ' + noun + ' history', response=noun + 'Page', page=True)
    op(public, '/tickets/{id}/' + route, 'post', 'add' + noun, 'Same application service as AI-approved operation',
       request, response, 201, idem=True)
op(public, '/tickets/{id}/transitions', 'post', 'transitionTicket', 'Explicit state-machine transition; conditional required reason',
   'Transition', 'Ticket', idem=True)
op(public, '/tickets/{id}/status-history', 'get', 'statusHistory', 'Authorized ticket state history', response='StatusHistoryPage', page=True)
op(public, '/audit-logs', 'get', 'auditLogs', 'Administrator only; redacted metadata', response='AuditPage', page=True,
   filters={'actorId': ID, 'action': string(80), 'resourceType': string(64), 'resourceId': string(64),
            'requestId': string(64), 'from': TIME, 'to': TIME})
op(public, '/health', 'get', 'healthJava', 'Minimal liveness only', response='Health', security=[])
# These resources do not share a createdAt ordering field.
for path in ['/departments', '/knowledge-bases/{id}/members']:
    for param in public['paths'][path]['get']['parameters']:
        if param['name'] == 'sort':
            param['schema'] = enum('id,asc', 'id,desc')

# Internal protocols carry identity only via a server-issued opaque context, never tool arguments.
S['Context'] = obj({'contextId': ID, 'conversationId': ID, 'turnId': ID, 'securityEpoch': INT,
                    'expiresAt': TIME, 'allowedKnowledgeBaseIds': arr(ID, 100),
                    'allowedVersions': arr(obj({'versionId': ID, 'indexGeneration': ID}), 1000),
                    'allowedTools': arr(enum(*tool_args), 7)})
S['EvidenceRequest'] = obj({'contextId': ID, 'securityEpoch': INT, 'chunkIds': arr(ID, 8)})
S['Evidence'] = obj({'chunkId': ID, 'versionId': ID, 'indexGeneration': ID,
                     'documentId': ID, 'knowledgeBaseId': ID, 'text': string(10000), 'textSha256': HASH,
                     'title': string(200), 'pageStart': nullable({'type': 'integer', 'minimum': 1}),
                     'pageEnd': nullable({'type': 'integer', 'minimum': 1}), 'headingPath': nullable(string(1000))})
S['EvidenceResponse'] = obj({'securityEpoch': INT, 'evidence': arr(ref('Evidence'), 8)})
S['InternalDraft'] = obj({'contextId': ID, 'toolCallId': ID, 'call': ref('WriteToolCall')})
S['ReadToolCall'] = {'oneOf': [obj({'name': {'const': n}, 'arguments': ref(n)}) for n in ['getTicket', 'listTickets']]}
S['InternalRead'] = obj({'contextId': ID, 'toolCallId': ID, 'call': ref('ReadToolCall')})
S['ReadResult'] = {'oneOf': [ref('Ticket'), ref('TicketPage')]}
op(internal, '/contexts/{contextId}', 'get', 'getAIContext', 'AI service only; verify context expiry/session/epoch', response='Context')
op(internal, '/evidence', 'post', 'getEvidence', 'Reauthorize exact chunks before prompt; all-or-error', 'EvidenceRequest', 'EvidenceResponse')
op(internal, '/tools/read', 'post', 'readTool', 'Controlled resource-scoped ticket reads', 'InternalRead', 'ReadResult')
op(internal, '/tool-requests', 'post', 'internalToolRequest', 'Create draft only from registered model call; no approval/execute endpoint',
   'InternalDraft', 'DraftResult', 201)
S['RagRequest'] = obj({'contextId': ID, 'question': string(2000), 'knowledgeBaseIds': arr(ID, 100)},
                     ['contextId', 'question'])
S['RagResult'] = obj({'answer': {'type': 'string', 'maxLength': 16000},
                      'outcome': enum('ANSWERED', 'INSUFFICIENT_EVIDENCE', 'INVALID_CITATION'),
                      'claims': arr(ref('Claim'), 30), 'citations': arr(ref('Citation'), 8),
                      'securityEpoch': INT, 'usage': ref('Usage')})
S['Usage'] = obj({'provider': string(80), 'modelRevision': string(200), 'promptTokens': INT,
                  'completionTokens': INT, 'durationMs': INT})
S['ProcessRequest'] = obj({'jobId': ID, 'leaseToken': ID, 'versionId': ID, 'documentId': ID,
                           'knowledgeBaseId': ID, 'indexGeneration': ID,
                           'storageKey': string(255, pattern='^[a-zA-Z0-9][a-zA-Z0-9/_-]*$'),
                           'sourceSha256': HASH, 'mediaType': enum('application/pdf',
                             'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                             'text/plain', 'text/markdown'),
                           'embeddingRevision': string(200)})
S['ChunkManifestItem'] = obj({'chunkId': ID, 'ordinal': INT, 'text': string(10000), 'textSha256': HASH,
                              'tokenCount': INT, 'pageStart': nullable({'type': 'integer', 'minimum': 1}),
                              'pageEnd': nullable({'type': 'integer', 'minimum': 1}),
                              'headingPath': nullable(string(1000)), 'charStart': INT, 'charEnd': INT})
S['ProcessResult'] = obj({'jobId': ID, 'leaseToken': ID, 'versionId': ID, 'indexGeneration': ID,
                          'sourceSha256': HASH, 'parserRevision': string(100), 'chunkerRevision': string(100),
                          'embeddingRevision': string(200), 'chunks': {**arr(ref('ChunkManifestItem'), 2000), 'minItems': 1}})
S['PlanRequest'] = obj({'contextId': ID, 'conversationId': ID, 'turnId': ID, 'question': string(2000),
                        'history': arr(obj({'role': enum('USER', 'ASSISTANT'), 'content': string(16000)}), 12)})
S['PlanResult'] = {'oneOf': [obj({'kind': {'const': 'TOOL'}, 'toolCallId': ID, 'call': ref('ToolCall')}),
                            obj({'kind': {'const': 'FINAL'}, 'answer': string(16000),
                                 'citations': arr(ref('Citation'), 8)})]}
S['ToolResultRequest'] = obj({'contextId': ID, 'conversationId': ID, 'turnId': ID,
                              'toolCallId': ID, 'question': string(2000),
                              'history': arr(obj({'role': enum('USER', 'ASSISTANT'), 'content': string(16000)}), 12),
                              'toolHistory': arr(obj({'toolCallId': ID, 'call': ref('ToolCall'),
                                'result': {'oneOf': [ref('ReadResult'), ref('SearchResult'), ref('DraftResult'), ref('Error')]}}), 3)})
op(ai, '/rag/query', 'post', 'ragQuery', 'Service-only RAG with mandatory Java context', 'RagRequest', 'RagResult')
op(ai, '/rag/search', 'post', 'ragSearch', 'Retrieval only; same mandatory authorization filter', 'RagRequest', 'SearchResult')
op(ai, '/documents/process', 'post', 'processDocument', 'Bounded synchronous processing called by durable Java worker',
   'ProcessRequest', 'ProcessResult')
op(ai, '/agent/plan', 'post', 'planAgent', 'One proposal step; cannot execute business writes', 'PlanRequest', 'PlanResult')
op(ai, '/agent/tool-result', 'post', 'agentToolResult', 'Consume validated result and propose next step', 'ToolResultRequest', 'PlanResult')
op(ai, '/health', 'get', 'healthAI', 'Minimal liveness; service remains network-internal', response='Health', security=[])


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, value in [('java-public.openapi.json', public), ('java-internal.openapi.json', internal),
                             ('ai-service.openapi.json', ai), ('tool-schemas.json', tools_doc)]:
        # Retain only transitive schema dependencies in each OpenAPI document.
        value = copy.deepcopy(value)
        if 'openapi' in value:
            needed = set()

            def visit(node):
                if isinstance(node, dict):
                    r = node.get('$ref', '')
                    if r.startswith('#/components/schemas/'):
                        name = r.rsplit('/', 1)[-1]
                        if name not in needed:
                            needed.add(name)
                            visit(S[name])
                    for child in node.values():
                        visit(child)
                elif isinstance(node, list):
                    for child in node:
                        visit(child)

            visit(value['paths'])
            value['components']['schemas'] = {key: S[key] for key in sorted(needed)}
        (OUT / filename).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
