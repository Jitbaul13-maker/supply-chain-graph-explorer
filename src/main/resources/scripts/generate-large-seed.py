from pathlib import Path

OUT = Path("src/main/resources/scripts/seed-large.cypher")

lines = []

def add(line=""):
    lines.append(line)

# ============================================================
# LARGE DEVELOPMENT DATASET
# ============================================================

add("// ============================================================")
add("// Large development seed")
add("// Generated deterministically")
add("// ============================================================")
add()

# ------------------------------------------------------------
# Services - 100
# ------------------------------------------------------------

add("// ---------- Services ----------")
for i in range(1, 101):
    service_id = f"service:svc-{i:03d}"
    name = f"Service {i:03d}"

    add(
        f"MERGE (s:Service {{id: '{service_id}'}}) "
        f"SET s.name = '{name}';"
    )

add()

# ------------------------------------------------------------
# Dependencies - 50
# ------------------------------------------------------------

add("// ---------- Dependencies ----------")
dependency_types = ["Database", "External Service", "Message Broker", "Cache"]

for i in range(1, 51):
    dependency_id = f"dependency:dep-{i:03d}"
    name = f"Dependency {i:03d}"
    dep_type = dependency_types[(i - 1) % len(dependency_types)]

    add(
        f"MERGE (d:Dependency {{id: '{dependency_id}'}}) "
        f"SET d.name = '{name}', d.type = '{dep_type}';"
    )

add()

# ------------------------------------------------------------
# Teams - 20
# ------------------------------------------------------------

add("// ---------- Teams ----------")
for i in range(1, 21):
    team_id = f"team:team-{i:02d}"
    name = f"Team {i:02d}"

    add(
        f"MERGE (t:Team {{id: '{team_id}'}}) "
        f"SET t.name = '{name}';"
    )

add()

# ------------------------------------------------------------
# Environments - 5
# ------------------------------------------------------------

add("// ---------- Environments ----------")
environments = ["production", "staging", "qa", "development", "sandbox"]

for env in environments:
    add(
        f"MERGE (e:Environment {{id: 'environment:{env}'}}) "
        f"SET e.name = '{env.capitalize()}';"
    )

add()

# ------------------------------------------------------------
# Regions - 10
# ------------------------------------------------------------

add("// ---------- Regions ----------")
regions = [
    "india",
    "us-east",
    "us-west",
    "eu-west",
    "eu-central",
    "ap-south",
    "ap-southeast",
    "australia",
    "canada",
    "uk",
]

for region in regions:
    add(
        f"MERGE (r:Region {{id: 'region:{region}'}}) "
        f"SET r.name = '{region.replace('-', ' ').title()}';"
    )

add()

# ------------------------------------------------------------
# Incidents - 100
# ------------------------------------------------------------

add("// ---------- Incidents ----------")
severities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

for i in range(1, 101):
    incident_id = f"incident:inc-{i:03d}"
    name = f"Incident {i:03d}"
    severity = severities[(i - 1) % len(severities)]

    add(
        f"MERGE (i:Incident {{id: '{incident_id}'}}) "
        f"SET i.name = '{name}', i.severity = '{severity}';"
    )

add()

# ============================================================
# RELATIONSHIPS
# ============================================================

# ------------------------------------------------------------
# Service -> Service
# Create a connected dependency chain for multi-hop traversal.
# ------------------------------------------------------------

add("// ---------- Service dependencies ----------")

for i in range(1, 100):
    next_service = i + 1

    add(
        f"MATCH (s1:Service {{id: 'service:svc-{i:03d}'}}), "
        f"(s2:Service {{id: 'service:svc-{next_service:03d}'}}) "
        f"MERGE (s1)-[:SERVICE_DEPENDS_ON]->(s2);"
    )

    if i + 2 <= 100:
        second_service = i + 2

        add(
            f"MATCH (s1:Service {{id: 'service:svc-{i:03d}'}}), "
            f"(s2:Service {{id: 'service:svc-{second_service:03d}'}}) "
            f"MERGE (s1)-[:SERVICE_DEPENDS_ON]->(s2);"
        )

add()

# ------------------------------------------------------------
# Service -> Dependency
# Each service gets 2 dependencies.
# ------------------------------------------------------------

add("// ---------- Service dependencies ----------")

for i in range(1, 101):
    dep1 = ((i - 1) % 50) + 1
    dep2 = (i % 50) + 1

    add(
        f"MATCH (s:Service {{id: 'service:svc-{i:03d}'}}), "
        f"(d:Dependency {{id: 'dependency:dep-{dep1:03d}'}}) "
        f"MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);"
    )

    if dep2 != dep1:
        add(
            f"MATCH (s:Service {{id: 'service:svc-{i:03d}'}}), "
            f"(d:Dependency {{id: 'dependency:dep-{dep2:03d}'}}) "
            f"MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);"
        )

add()

# ------------------------------------------------------------
# Service -> Team
# ------------------------------------------------------------

add("// ---------- Ownership ----------")

for i in range(1, 101):
    team = ((i - 1) % 20) + 1

    add(
        f"MATCH (s:Service {{id: 'service:svc-{i:03d}'}}), "
        f"(t:Team {{id: 'team:team-{team:02d}'}}) "
        f"MERGE (s)-[:OWNED_BY]->(t);"
    )

add()

# ------------------------------------------------------------
# Service -> Environment
# ------------------------------------------------------------

add("// ---------- Deployment ----------")

for i in range(1, 101):
    env = environments[(i - 1) % len(environments)]

    add(
        f"MATCH (s:Service {{id: 'service:svc-{i:03d}'}}), "
        f"(e:Environment {{id: 'environment:{env}'}}) "
        f"MERGE (s)-[:DEPLOYED_IN]->(e);"
    )

add()

# ------------------------------------------------------------
# Environment -> Region
# ------------------------------------------------------------

add("// ---------- Environment -> Region ----------")

for i, env in enumerate(environments):
    region = regions[i % len(regions)]

    add(
        f"MATCH (e:Environment {{id: 'environment:{env}'}}), "
        f"(r:Region {{id: 'region:{region}'}}) "
        f"MERGE (e)-[:RUNS_ON]->(r);"
    )

add()

# ------------------------------------------------------------
# Incidents -> Services
# ------------------------------------------------------------

add("// ---------- Incident impact ----------")

for i in range(1, 101):
    service = ((i - 1) % 100) + 1

    add(
        f"MATCH (i:Incident {{id: 'incident:inc-{i:03d}'}}), "
        f"(s:Service {{id: 'service:svc-{service:03d}'}}) "
        f"MERGE (i)-[:AFFECTS]->(s);"
    )

add()

# ------------------------------------------------------------
# Dependency relationships
# ------------------------------------------------------------

add("// ---------- Dependency relationships ----------")

for i in range(1, 50):
    add(
        f"MATCH (d1:Dependency {{id: 'dependency:dep-{i:03d}'}}), "
        f"(d2:Dependency {{id: 'dependency:dep-{i + 1:03d}'}}) "
        f"MERGE (d1)-[:RELATED_TO]->(d2);"
    )

add()

# ============================================================
# Preserve the canonical payment-gateway scenario
# ============================================================

add("// ============================================================")
add("// Canonical payment-gateway scenario")
add("// ============================================================")
add()

# Canonical nodes
add(
    "MERGE (s:Service {id: 'service:payment'}) "
    "SET s.name = 'Payment Service';"
)

add(
    "MERGE (d:Dependency {id: 'dependency:payment-gateway'}) "
    "SET d.name = 'Payment Gateway', d.type = 'External Service';"
)

add(
    "MERGE (d:Dependency {id: 'dependency:postgres'}) "
    "SET d.name = 'PostgreSQL', d.type = 'Database';"
)

add(
    "MERGE (d:Dependency {id: 'dependency:shipping-api'}) "
    "SET d.name = 'Shipping API', d.type = 'External Service';"
)

add(
    "MERGE (s:Service {id: 'service:order'}) "
    "SET s.name = 'Order Service';"
)

add(
    "MERGE (s:Service {id: 'service:inventory'}) "
    "SET s.name = 'Inventory Service';"
)

add(
    "MERGE (s:Service {id: 'service:shipping'}) "
    "SET s.name = 'Shipping Service';"
)

add(
    "MERGE (s:Service {id: 'service:user'}) "
    "SET s.name = 'User Service';"
)

add(
    "MERGE (t:Team {id: 'team:payments'}) "
    "SET t.name = 'Payments Team';"
)

add(
    "MERGE (t:Team {id: 'team:orders'}) "
    "SET t.name = 'Orders Team';"
)

add(
    "MERGE (t:Team {id: 'team:platform'}) "
    "SET t.name = 'Platform Team';"
)

add(
    "MERGE (t:Team {id: 'team:logistics'}) "
    "SET t.name = 'Logistics Team';"
)

add(
    "MERGE (e:Environment {id: 'environment:production'}) "
    "SET e.name = 'Production';"
)

add(
    "MERGE (e:Environment {id: 'environment:staging'}) "
    "SET e.name = 'Staging';"
)

add(
    "MERGE (r:Region {id: 'region:india'}) "
    "SET r.name = 'India';"
)

add(
    "MERGE (r:Region {id: 'region:us-east'}) "
    "SET r.name = 'US East';"
)

add(
    "MERGE (i:Incident {id: 'incident:payment-gateway-outage'}) "
    "SET i.name = 'Payment Gateway Outage', i.severity = 'CRITICAL';"
)

# Canonical relationships
canonical_relationships = [
    (
        "Service",
        "service:payment",
        "Dependency",
        "dependency:payment-gateway",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:payment",
        "Dependency",
        "dependency:postgres",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:order",
        "Service",
        "service:payment",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:order",
        "Service",
        "service:inventory",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:shipping",
        "Dependency",
        "dependency:shipping-api",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:user",
        "Dependency",
        "dependency:postgres",
        "SERVICE_DEPENDS_ON",
    ),
    (
        "Service",
        "service:payment",
        "Team",
        "team:payments",
        "OWNED_BY",
    ),
    (
        "Service",
        "service:order",
        "Team",
        "team:orders",
        "OWNED_BY",
    ),
    (
        "Service",
        "service:inventory",
        "Team",
        "team:platform",
        "OWNED_BY",
    ),
    (
        "Service",
        "service:shipping",
        "Team",
        "team:logistics",
        "OWNED_BY",
    ),
    (
        "Service",
        "service:user",
        "Team",
        "team:platform",
        "OWNED_BY",
    ),
    (
        "Service",
        "service:payment",
        "Environment",
        "environment:production",
        "DEPLOYED_IN",
    ),
    (
        "Service",
        "service:order",
        "Environment",
        "environment:production",
        "DEPLOYED_IN",
    ),
    (
        "Service",
        "service:inventory",
        "Environment",
        "environment:production",
        "DEPLOYED_IN",
    ),
    (
        "Service",
        "service:shipping",
        "Environment",
        "environment:production",
        "DEPLOYED_IN",
    ),
    (
        "Service",
        "service:user",
        "Environment",
        "environment:staging",
        "DEPLOYED_IN",
    ),
    (
        "Environment",
        "environment:production",
        "Region",
        "region:india",
        "RUNS_ON",
    ),
    (
        "Environment",
        "environment:staging",
        "Region",
        "region:us-east",
        "RUNS_ON",
    ),
    (
        "Incident",
        "incident:payment-gateway-outage",
        "Service",
        "service:payment",
        "AFFECTS",
    ),
    (
        "Incident",
        "incident:payment-gateway-outage",
        "Dependency",
        "dependency:payment-gateway",
        "AFFECTS",
    ),
    (
        "Dependency",
        "dependency:payment-gateway",
        "Dependency",
        "dependency:shipping-api",
        "RELATED_TO",
    ),
]

for label1, id1, label2, id2, relationship in canonical_relationships:
    add(
        f"MATCH (a:{label1} {{id: '{id1}'}}), "
        f"(b:{label2} {{id: '{id2}'}}) "
        f"MERGE (a)-[:{relationship}]->(b);"
    )

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

print(f"Generated: {OUT}")
print(f"Cypher statements: {sum(1 for line in lines if line.startswith(('MERGE', 'MATCH')))}")