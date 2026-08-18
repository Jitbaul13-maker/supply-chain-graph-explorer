// ===============================
// Nodes
// ===============================

// ---------- Services ----------

MERGE (s:Service {id: 'service:payment'})
SET s.name = 'Payment Service';

MERGE (s:Service {id: 'service:order'})
SET s.name = 'Order Service';

MERGE (s:Service {id: 'service:inventory'})
SET s.name = 'Inventory Service';

MERGE (s:Service {id: 'service:shipping'})
SET s.name = 'Shipping Service';

MERGE (s:Service {id: 'service:user'})
SET s.name = 'User Service';


// ---------- Dependencies ----------

MERGE (d:Dependency {id: 'dependency:payment-gateway'})
SET d.name = 'Payment Gateway',
d.type = 'External Service';

MERGE (d:Dependency {id: 'dependency:postgres'})
SET d.name = 'PostgreSQL',
d.type = 'Database';

MERGE (d:Dependency {id: 'dependency:shipping-api'})
SET d.name = 'Shipping API',
d.type = 'External Service';


// ---------- Teams ----------

MERGE (t:Team {id: 'team:payments'})
SET t.name = 'Payments Team';

MERGE (t:Team {id: 'team:orders'})
SET t.name = 'Orders Team';

MERGE (t:Team {id: 'team:platform'})
SET t.name = 'Platform Team';

MERGE (t:Team {id: 'team:logistics'})
SET t.name = 'Logistics Team';


// ---------- Environments ----------

MERGE (e:Environment {id: 'environment:production'})
SET e.name = 'Production';

MERGE (e:Environment {id: 'environment:staging'})
SET e.name = 'Staging';


// ---------- Regions ----------

MERGE (r:Region {id: 'region:india'})
SET r.name = 'India';

MERGE (r:Region {id: 'region:us-east'})
SET r.name = 'US East';


// ---------- Incidents ----------

MERGE (i:Incident {id: 'incident:payment-gateway-outage'})
SET i.name = 'Payment Gateway Outage',
i.severity = 'CRITICAL';

// ===============================
// Relationships
// ===============================

// ---------- Service dependencies ----------

MATCH (s:Service {id: 'service:payment'}),
      (d:Dependency {id: 'dependency:payment-gateway'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);

MATCH (s:Service {id: 'service:payment'}),
      (d:Dependency {id: 'dependency:postgres'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);

MATCH (s:Service {id: 'service:order'}),
      (p:Service {id: 'service:payment'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(p);

MATCH (s:Service {id: 'service:order'}),
      (i:Service {id: 'service:inventory'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(i);

MATCH (s:Service {id: 'service:shipping'}),
      (d:Dependency {id: 'dependency:shipping-api'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);

MATCH (s:Service {id: 'service:user'}),
      (d:Dependency {id: 'dependency:postgres'})
MERGE (s)-[:SERVICE_DEPENDS_ON]->(d);


// ---------- Ownership ----------

MATCH (s:Service {id: 'service:payment'}),
      (t:Team {id: 'team:payments'})
MERGE (s)-[:OWNED_BY]->(t);

MATCH (s:Service {id: 'service:order'}),
      (t:Team {id: 'team:orders'})
MERGE (s)-[:OWNED_BY]->(t);

MATCH (s:Service {id: 'service:inventory'}),
      (t:Team {id: 'team:platform'})
MERGE (s)-[:OWNED_BY]->(t);

MATCH (s:Service {id: 'service:shipping'}),
      (t:Team {id: 'team:logistics'})
MERGE (s)-[:OWNED_BY]->(t);

MATCH (s:Service {id: 'service:user'}),
      (t:Team {id: 'team:platform'})
MERGE (s)-[:OWNED_BY]->(t);


// ---------- Deployment ----------

MATCH (s:Service {id: 'service:payment'}),
      (e:Environment {id: 'environment:production'})
MERGE (s)-[:DEPLOYED_IN]->(e);

MATCH (s:Service {id: 'service:order'}),
      (e:Environment {id: 'environment:production'})
MERGE (s)-[:DEPLOYED_IN]->(e);

MATCH (s:Service {id: 'service:inventory'}),
      (e:Environment {id: 'environment:production'})
MERGE (s)-[:DEPLOYED_IN]->(e);

MATCH (s:Service {id: 'service:shipping'}),
      (e:Environment {id: 'environment:production'})
MERGE (s)-[:DEPLOYED_IN]->(e);

MATCH (s:Service {id: 'service:user'}),
      (e:Environment {id: 'environment:staging'})
MERGE (s)-[:DEPLOYED_IN]->(e);


// ---------- Environment → Region ----------

MATCH (e:Environment {id: 'environment:production'}),
      (r:Region {id: 'region:india'})
MERGE (e)-[:RUNS_ON]->(r);

MATCH (e:Environment {id: 'environment:staging'}),
      (r:Region {id: 'region:us-east'})
MERGE (e)-[:RUNS_ON]->(r);


// ---------- Incident impact ----------

MATCH (i:Incident {id: 'incident:payment-gateway-outage'}),
      (s:Service {id: 'service:payment'})
MERGE (i)-[:AFFECTS]->(s);

MATCH (i:Incident {id: 'incident:payment-gateway-outage'}),
      (d:Dependency {id: 'dependency:payment-gateway'})
MERGE (i)-[:AFFECTS]->(d);


// ---------- Alternative / mitigation ----------

MATCH (d1:Dependency {id: 'dependency:payment-gateway'}),
      (d2:Dependency {id: 'dependency:shipping-api'})
MERGE (d1)-[:RELATED_TO]->(d2);