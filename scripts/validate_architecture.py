"""Validate architecture artifacts, not unimplemented runtime behavior."""
import copy
import json
import re
from collections import Counter
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker
from openapi_spec_validator import validate

ROOT = Path(__file__).resolve().parents[1]


def read_json(path):
    return json.loads((ROOT / path).read_text(encoding='utf-8'))


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    required = ['README.md', 'ARCHITECTURE_PHASE_REPORT.md', 'docker-compose.yml', '.env.example']
    required += ['docs/' + name + '.md' for name in ['PRODUCT_SCOPE', 'ARCHITECTURE', 'DOMAIN_MODEL',
                 'DATABASE_DESIGN', 'API_CONTRACT', 'AI_ARCHITECTURE', 'SECURITY_MODEL', 'TEST_STRATEGY', 'IMPLEMENTATION_PLAN']]
    for path in required:
        require((ROOT / path).is_file(), f'Missing {path}')
    adrs = list((ROOT / 'docs/ADR').glob('ADR-*.md'))
    require(len(adrs) >= 6, 'Missing ADRs')
    for path in adrs:
        for heading in ['Context', 'Decision', 'Alternatives', 'Consequences']:
            require('## ' + heading in path.read_text(encoding='utf-8'), f'{path}: missing {heading}')
    markdowns = [ROOT / 'README.md', ROOT / 'ARCHITECTURE_PHASE_REPORT.md']
    for folder in ['docs', 'frontend', 'backend-java', 'ai-service', 'evaluation', 'infra']:
        markdowns += list((ROOT / folder).rglob('*.md'))
    for path in markdowns:
        for target in re.findall(r'\[[^\]]+\]\(([^)]+)\)', path.read_text(encoding='utf-8')):
            if '://' in target or target.startswith('#'):
                continue
            require((path.parent / target.split('#')[0]).exists(), f'Broken link in {path}: {target}')
    print(f'PASS required documents, {len(adrs)} ADRs, {len(markdowns)} Markdown link sets')

    specs = []
    for path in sorted((ROOT / 'docs/contracts').glob('*.openapi.json')):
        spec = json.loads(path.read_text(encoding='utf-8'))
        validate(spec)
        operations = [op for item in spec['paths'].values() for method, op in item.items()
                      if method in {'get', 'post', 'put', 'patch', 'delete'}]
        require(len({op['operationId'] for op in operations}) == len(operations), 'Duplicate operationId')
        specs.append(spec)
        print(f'PASS OpenAPI {path.name}: {len(operations)} operations')
    require(len(specs) == 3, 'Require public/internal/Python specs')
    internal = read_json('docs/contracts/java-internal.openapi.json')
    require(not any('execute' in p or 'decision' in p for p in internal['paths']), 'AI must not have approval/execute API')

    tools = read_json('docs/contracts/tool-schemas.json')
    Draft202012Validator.check_schema(tools)
    validator = Draft202012Validator(tools, format_checker=FormatChecker())
    require(len(tools['$defs']) == 7, 'Exactly seven tools')
    forbidden = {'userId', 'requesterId', 'departmentId', 'adminRole', 'actorId', 'contextId', 'expectedVersion'}
    for name, schema in tools['$defs'].items():
        require(schema.get('additionalProperties') is False, f'Non-strict tool: {name}')
        require(not forbidden.intersection(schema['properties']), f'Identity field in {name}')
    valid = {'name': 'createTicketDraft', 'arguments': {'title': 'VPN issue', 'description': 'Cannot connect',
                                                      'category': 'IT', 'priority': 'NORMAL'}}
    validator.validate(valid)
    negative = []
    for key in forbidden:
        bad = copy.deepcopy(valid)
        bad['arguments'][key] = 'injected'
        negative.append(bad)
    bad = copy.deepcopy(valid)
    bad['name'] = 'executeTicket'
    negative.append(bad)
    for bad in negative:
        require(not validator.is_valid(bad), f'Tool injection accepted: {bad}')
    print(f'PASS strict tool contract: positive sample and {len(negative)} negative samples')

    corpus = read_json('evaluation/corpus.json')
    case_schema = read_json('evaluation/case.schema.json')
    Draft202012Validator.check_schema(case_schema)
    case_validator = Draft202012Validator(case_schema)
    cases = [json.loads(line) for line in (ROOT / 'evaluation/cases.jsonl').read_text(encoding='utf-8').splitlines() if line]
    require(30 <= len(cases) <= 50, 'Evaluation requires 30-50 cases')
    require(len({c['id'] for c in cases}) == len(cases), 'Duplicate case id')
    sources = {d['sourceId']: d for d in corpus['documents']}
    require(len(sources) == len(corpus['documents']), 'Duplicate source ID')
    for case in cases:
        case_validator.validate(case)
        require(case['actor'] in corpus['actors'], 'Unknown actor')
        actor = corpus['actors'][case['actor']]
        expected = case['expected']
        for gold in expected['goldEvidence']:
            require(gold['sourceId'] in sources, 'Unknown gold source')
            source = sources[gold['sourceId']]
            require(source['status'] == 'ACTIVE' and source['isCurrent'], 'Gold retrieval points to non-current source')
            require(source['knowledgeBase'] in actor['memberships'], 'Gold source inaccessible to actor')
            require(gold['anchor'] in {b['anchor'] for b in source['blocks']}, 'Unknown gold anchor')
        if expected['tool'] != 'NONE':
            validator.validate({'name': expected['tool'], 'arguments': expected['arguments']})
        require(expected['approvalRequired'] == expected['tool'].endswith('Draft'), 'Approval expectation mismatch')
    print(f'PASS evaluation: {len(cases)} cases, splits {dict(Counter(c["split"] for c in cases))}, {len(sources)} sources')

    sql = (ROOT / 'docs/database/schema.sql').read_text(encoding='utf-8')
    erd = (ROOT / 'docs/database/ERD.md').read_text(encoding='utf-8')
    tables = re.findall(r'CREATE TABLE (\w+) \(', sql)
    require(len(tables) == len(set(tables)), 'Duplicate SQL table')
    for table in tables:
        require(re.search(r'\b' + table + r' \{', erd), f'Table missing from ERD: {table}')
    for target in re.findall(r'REFERENCES (\w+)\(', sql):
        require(target in tables, f'Unknown FK table: {target}')
    require(len(re.findall(r'REFERENCES \w+\(', sql)) == len(re.findall(r'--o[|{]', erd)), 'ERD FK coverage mismatch')
    require('approval_id CHAR(36) UNIQUE' in sql, 'Missing permanent execution idempotency')
    require('FOREIGN KEY (id,current_version_id)' in sql, 'Missing same-document pointer invariant')
    print(f'PASS SQL/ERD structural coverage: {len(tables)} tables; SQL execution NOT tested')

    compose = yaml.safe_load((ROOT / 'docker-compose.yml').read_text(encoding='utf-8'))
    apps = yaml.safe_load((ROOT / 'infra/compose.application.yml').read_text(encoding='utf-8'))
    require(set(compose['services']) == {'java-backend', 'mysql', 'redis', 'vector-store'},
            'Unexpected Phase 1 services')
    java = compose['services']['java-backend']
    require(set(java['networks']) == {'web', 'business-data'}, 'Java network boundary mismatch')
    require(java['ports'][0].startswith('127.0.0.1:'), 'Java port must bind to loopback')
    require(set(apps['services']) == {'frontend', 'java-backend', 'ai-service'}, 'Unexpected application services')
    py = apps['services']['ai-service']
    require('business-data' not in py['networks'], 'Python can reach MySQL data network')
    require(not any('MYSQL' in k or 'REDIS' in k for k in py['environment']), 'Python has business DB configuration')
    require('ports' not in py, 'Python has browser-facing published port')
    require('document-files:/data/documents:ro' in py['volumes'], 'Python file volume must be read-only')
    print('PASS Compose security layout; image startup NOT tested')
    print('ARCHITECTURE_STATIC_VALIDATION=PASS')


if __name__ == '__main__':
    main()
