/*
Copyright (c) 2021 Daniel Murygin.

This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty
of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.
If not, see <http://www.gnu.org/licenses/>.

------------------------------------------------------------------------

Test script for the load test tool k6: https://k6.io/.

To run the script behind a proxy set environment variables before executing:

 export HTTPS_PROXY=http://<HOST>:<PORT>
 export HTTP_PROXY=http://<HOST>:<PORT>

Parameters to run the script:

 -e name=<USER_NAME> Username of an account for veo (required)
 -e password=<PASSWORD> Password of an account for veo (required)
 -e host=<HOSTNAME> Hostname of the veo services (e.g. verinice.com)
 -e keycloak_url=<KEYCLOAK_BASE_URL> Keycloak base URL (e.g. https://auth.verinice.com)
 -e keycloak_client=<KEYCLOAK_CLIENT_ID> Keycloak Client ID (e.g. veo-prod)
 -e unit=<UNIT_ID> The ID of the unit that will be used for the test.

After installing k6, this script is started with:

 k6 run create_processing_activity.js -e host=verinice.com -e keycloak_url=https://auth.verinice.com -e keycloak_client=veo-prod -e name=foo -e password=bar

To run a load test with 10 virtual user (vus) for 30s, type:

 k6 run --vus 10 --duration 30s create_processing_activity.js -e host=verinice.com -e keycloak_url=https://auth.verinice.com -e keycloak_client=veo-prod -e name=foo -e password=bar

See k6 documentation for more options: 
https://k6.io/docs/getting-started/running-k6
*/
import { check } from "k6";
import { sleep } from "k6";
import { IFrameElement } from "k6/html";
import { URL } from 'https://jslib.k6.io/url/1.0.0/index.js';
import http from "k6/http";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

const HOSTNAME = __ENV.host;
const USER_NAME = __ENV.name;
const PASSWORD = __ENV.password;
const KEYCLOAK_BASE_URL = __ENV.keycloak_url;
const KEYCLOAK_CLIENT_ID = __ENV.keycloak_client; 
// ID of the unit being processed
let unitId = __ENV.unit; 

// Base URLs of the APIs
const VEO_BASE_URL = "https://api." + HOSTNAME + "/veo";
const VEO_FORMS_BASE_URL = "https://api." + HOSTNAME + "/forms";
const VEO_HISTORY_BASE_URL = "https://api." + HOSTNAME + "/history";
const VEO_REPORTS_BASE_URL = "https://api." + HOSTNAME + "/reporting";

// Keycloak authentication parameter
const KEYCLOAK_REALM = "verinice-veo";

// Maximum number of seconds to sleep after a request
const MAX_SLEEP_SECONDS = 5;

// Maximum number of seconds to sleep before saving a new element
const MAX_SLEEP_SECONDS_NEW_ELEMENT = 15;

const REQUEST_TIMEOUT = "8s";

const RESPONSIBLE_BODY = JSON.parse(open("./responsible_body.json"));
const JOINT_CONTROLLER = JSON.parse(open("./joint_controller.json"));
const PERSON = JSON.parse(open("./person.json"));
const TOM = JSON.parse(open("./tom.json"));
const DATA_TYPE = JSON.parse(open("./data_type.json"));
const DATA_TRANSFER = JSON.parse(open("./data_transfer.json"));
const IT_SYSTEM = JSON.parse(open("./it_system.json"));
const APPLICATION = JSON.parse(open("./application.json"));
let scenario = JSON.parse(open("./scenario.json"));
const DATA_PROCESSING = JSON.parse(open("./data_processing.json"));
let risk = JSON.parse(open("./risk.json"));

// Token for Authorization "Bearer <TOKEN>"
let TOKEN;



// ID of the domain being processed
let domainId;

let scenarioIds = [];
let scenarioRiskIds = [];
let personIds = [];
let tomIds = [];
let dataProcessingId;
let itSystemIds = [];
let applicationIds = [];

export let options = {
  thresholds: {
    http_req_duration: ["p(99)<3000"], // 99% of requests must complete below 3s
    http_req_duration: ["p(95)<1000"], // 95% of requests must complete below 1s
    http_req_duration: ["p(80)<200"] // 80% of requests must complete below 200ms
  },
  stages: [
    { duration: "2m", target: 100 }, // Scale up
    { duration: "8m", target: 100 }, 
    { duration: "2m", target: 0 } // Scale down
  ],
  ext: {
    loadimpact: {
      projectID: 3621988,
      // Test runs with the same name groups test runs together
      name: "Create Processing Activity",distribution: {
        distributionLabel1: { loadZone: 'amazon:de:frankfurt', percent: 100 }
      }
    }
  }
};

// This method is executed for each virtual user
export default function () {
  console.info("Starting iteration...");
  getToken();
  var domain = loadFirstDomain();
  domainId = domain.id;
  check(domainId, {
    "Domain ID is a valid UUID": (id) => checkIfValidUUID(id),
  });

  if(!checkIfValidUUID(domainId)) {
    console.error("Domain ID is not a valid UUID: " + domainId);
  }
  if(unitId===undefined) {
    loadUnitSelection();
  }
  loadDashboard();
  let numberOfScenarios = getRandomInt(10) + 1;
  for (let i = 0; i < numberOfScenarios; i++) {
    scenarioIds.push(createScenario());
  }

  scenarioRiskIds = [...scenarioIds];
  let responsibleBodyId = createResponsibleBody();
  let jointControllerId = createJointController();
  loadDashboard();
  let numberOfPersons = getRandomInt(5) + 1;
  for (let i = 0; i < numberOfPersons; i++) {
    personIds.push(createPerson());
  }
  let numberOfToms = getRandomInt(8) + 1;
  for (let i = 0; i < numberOfToms; i++) {
    tomIds.push(createTOM());
  }
  let dataTypeId = createDataType();
  DATA_TRANSFER.links.process_dataType[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + dataTypeId;
  let dataTransferId = createDataTransfer();
  loadDashboard();
  getToken();
  let numberOfItSystems = getRandomInt(4) + 1;
  for (let i = 0; i < numberOfItSystems; i++) {
    itSystemIds.push(createItSystem());
  }
  let numberOfApplications = getRandomInt(6) + 1;
  for (let i = 0; i < numberOfApplications; i++) {
    applicationIds.push(createApplication());
  }
  loadDashboard();
  DATA_PROCESSING.links.process_responsiblePerson[0].target.targetUri = "https://api." + HOSTNAME + "/veo/persons/" + getRandom(personIds);
  DATA_PROCESSING.links.process_responsibleBody[0].target.targetUri = "https://api." + HOSTNAME + "/veo/scopes/" + responsibleBodyId;
  DATA_PROCESSING.links.process_jointControllership[0].target.targetUri = "https://api." + HOSTNAME + "/veo/scopes/" + jointControllerId;
  DATA_PROCESSING.links.process_dataType[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + dataTypeId;
  DATA_PROCESSING.links.process_requiredApplications[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + getRandom(applicationIds);
  DATA_PROCESSING.links.process_requiredITSystems[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + getRandom(itSystemIds);
  DATA_PROCESSING.links.process_dataTransmission[0].target.targetUri = "https://api." + HOSTNAME + "/veo/processes/" + dataTransferId;
  dataProcessingId = createDataProcessing();
  let numberOfRisks = getRandomInt(scenarioIds.length);
  for (let i = 0; i < numberOfRisks; i++) {
    createRisk(dataProcessingId);
  }
  
  deleteElement("/processes/", dataProcessingId);
  deleteElement("/processes/", dataTransferId);
  deleteElement("/scopes/", responsibleBodyId);
  deleteElement("/scopes/", jointControllerId);
  personIds.forEach(id => {deleteElement("/persons/", id)});
  personIds = [];
  tomIds.forEach(id => {deleteElement("/controls/", id)});
  tomIds = [];
  deleteElement("/assets/", dataTypeId);
  itSystemIds.forEach(id => {deleteElement("/assets/", id)});
  itSystemIds = [];
  applicationIds.forEach(id => {deleteElement("/assets/", id)});
  applicationIds = [];
  scenarioIds.forEach(id => {deleteElement("/scenarios/", id)});
  scenarioIds = [];
}

/**
 * Creates an HTML report 
 * See: https://github.com/benc-uk/k6-reporter
 */
/*
export function handleSummary(data) {
  return {
    "summary.html": htmlReport(data),
  };
}
*/

export function loadUnitSelection() {
  console.info("Loading unit selection...");
  loadElementsAndSleep("/domains", 0);
  loadElementsAndSleep("/units", 0);
  loadElementsAndSleep("/domains", 0);
  loadElementsAndSleep("/units", 0);
  loadElementsAndSleep("/types", 0);

  var unitName = "Unit 1";


  var unit = searchUnit(unitName);

  check(unit, {
    "Unit found": (r) => typeof unit != "undefined",
    "Unit has correct name": (r) => unit.name == unitName,
  });

  unitId = unit.id;
  loadElement("/units/", unitId);
}

export function loadDashboard() {
  console.info("Loading dashboard...");

  loadElementAndSleep("/domains/", domainId, 0);
  loadElementAndSleep("/domains/", domainId, 0);
  loadElementAndSleep("/domains/", domainId, 0);
  loadElementStatusCount(unitId) 
  loadSchema("process"); 
  loadElementsAndSleep("/domains", 0); 
  loadTranslations()
  loadSchema("incident");
  loadForms();
  loadSchema("document");
  loadTranslations()
  loadForms();
  loadSchema("scenario");
  loadTranslations()
  loadForms();
  loadSchema("scope");
  loadHistory(unitId);
  loadTranslations()
  loadForms();
  loadForms();
  loadTranslations()
  loadSchema("asset");
  loadTranslations()
  loadForms();
  loadTranslations()
  loadSchema("person");
  loadForms();
  loadSchema("control");
  loadTranslations()
  loadForms();
  loadForms();
}

export function loadElementStatusCount(unitId) {
  loadElementsAndSleep("/domains/" + domainId + "/element-status-count?unit=" + unitId, 0); // 3x
}

export function createRisk(processId) {
  console.info("Creating risk...");
  risk.scenario.targetUri = "https://api." + HOSTNAME + "/veo/scenarios/" + scenarioRiskIds.pop();
  risk.mitigation.targetUri = "https://api." + HOSTNAME + "/veo/controls/" + getRandom(tomIds);
  risk.riskOwner.targetUri = "https://api." + HOSTNAME + "/veo/persons/" + getRandom(personIds);
  risk.process.targetUri = "https://api." + HOSTNAME + "/veo/processes/" + dataProcessingId;
  let riskString = JSON.stringify(risk);
  riskString = riskString.replace("DOMAIN_ID", domainId);
  riskString = riskString.replace("DOMAIN_ID", domainId);
  riskString = riskString.replace("\"SPECIFIC_PROBABILITY\"", getRandomInt(3));
  riskString = riskString.replace("\"SPECIFIC_IMPACT_I\"", getRandomInt(3));
  riskString = riskString.replace("\"SPECIFIC_IMPACT_A\"", getRandomInt(3));
  riskString = riskString.replace("\"SPECIFIC_IMPACT_R\"", getRandomInt(3));
  riskString = riskString.replace("\"SPECIFIC_IMPACT_C\"", getRandomInt(3));
  riskString = riskString.replace("\"RESIDUAL_RISK_I\"", getRandomInt(3));
  riskString = riskString.replace("\"RESIDUAL_RISK_A\"", getRandomInt(3));
  riskString = riskString.replace("\"RESIDUAL_RISK_R\"", getRandomInt(3));
  riskString = riskString.replace("\"RESIDUAL_RISK_C\"", getRandomInt(3));
  riskString = riskString.replace("\"SPECIFIC_PROBABILITY\"", getRandomInt(3));
  risk = JSON.parse(riskString);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/processes/" + processId + "/risks", risk, undefined,domainId,undefined);
}

export function createDataProcessing() {
  console.info("Creating data processing...");
  loadForms();
  loadProcesses(unitId, "PRO_DataProcessing");
  loadSchema("process");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/processes", DATA_PROCESSING, "PRO_DataProcessing",domainId,unitId);
}

export function createApplication() {
  console.info("Creating application...");
  loadForms();
  loadAssets(unitId, "AST_Application");
  loadSchema("asset");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/assets", APPLICATION, "AST_Application",domainId,unitId);
}

export function createItSystem() {
  console.info("Creating it system...");
  loadForms();
  loadAssets(unitId, "AST_IT-System");
  loadSchema("asset");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/assets", IT_SYSTEM, "AST_IT-System",domainId,unitId);
}

export function createScenario() {
  console.info("Creating scenario...");
  loadForms();
  loadScenarios(unitId, "SCN_Scenario");
  loadSchema("scenario");
  loadHistory(unitId);
  let scenarioString = JSON.stringify(scenario);
  scenarioString = scenarioString.replace("DOMAIN_ID", domainId);
  scenarioString = scenarioString.replace("\"PROBABILITY\"", getRandomInt(3));
  scenario = JSON.parse(scenarioString);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/scenarios", scenario, undefined, domainId,unitId);
}

export function createDataTransfer() {
  console.info("Creating data transfer...");
  loadForms();
  loadScopes(unitId, "PRO_DataTransfer");
  loadSchema("process");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/processes", DATA_TRANSFER, "PRO_DataTransfer",domainId,unitId);
}

export function createDataType() {
  console.info("Creating data type...");
  loadForms();
  loadAssets(unitId, "AST_Datatype");
  loadSchema("asset");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/assets", DATA_TYPE, "AST_Datatype",domainId,unitId);
}

export function createTOM() {
  console.info("Creating TOM...");
  loadForms();
  loadControls(unitId, "CTL_TOM");
  loadSchema("control");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/controls", TOM, "CTL_TOM",domainId,unitId);
}

export function createPerson() {
  console.info("Creating person...");
  loadForms();
  loadPersons(unitId, "PER_Person");
  loadSchema("person");
  loadHistory(unitId);
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/persons", PERSON, "PER_Person",domainId,unitId);
}

export function createResponsibleBody() {
  console.info("Creating responsible body...");
  loadForms();
  loadScopes(unitId, "SCP_ResponsibleBody");
  loadSchema("scope");
  loadPersons(unitId, "PER_DataProtectionOfficer");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_DataProtectionOfficer");
  loadPersons(unitId, "PER_Person");
  loadHistory(unitId);
  loadForms();
  loadForms();
  loadForms();
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/scopes", RESPONSIBLE_BODY, "SCP_ResponsibleBody",domainId,unitId);
}

export function createJointController() {
  console.info("Creating joint controller...");
  loadForms();
  loadScopes(unitId, "SCP_JointController");
  loadSchema("scope");
  loadPersons(unitId, "PER_DataProtectionOfficer");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_Person");
  loadPersons(unitId, "PER_DataProtectionOfficer");
  loadPersons(unitId, "PER_Person");
  loadHistory(unitId);
  loadForms();
  loadForms();
  loadForms();
  sleep(Math.random() * MAX_SLEEP_SECONDS_NEW_ELEMENT);
  return createElement("/scopes", JOINT_CONTROLLER, "SCP_JointController",domainId,unitId);
}

export function createElement(path, body, subType, domainId, unitId) {
  if(!(unitId===undefined)) {
    body.owner.targetUri = VEO_BASE_URL + "/units/" + unitId;
  }
  if(!(subType===undefined)) {
    body.domains[domainId] = {"subType":subType,"status":"NEW"};
  }
  var url = VEO_BASE_URL + path;
  var tag = path;
  if(tag.includes("risk")) {
    tag = "/processes/ID/risks"
  }
  var params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: TOKEN
    },
    tags: { 
      name: 'POST ' + tag
    },
    timeout: REQUEST_TIMEOUT
  };
  var result = http.post(url, JSON.stringify(body), params);

  check(result, {
    "Create element result is status 201": (result) => result.status === 201,
  });
  if (result.status != 201) {
    console.error("Create element status: " + result.status);
    console.error("Create element path: " + path);
    console.error("Create element body:");
    console.error(JSON.stringify(body));
  }
  var elementId = result.json().resourceId;
  check(elementId, {
    "Element ID is returned": (r) => elementId.length == 36,
  });
  return elementId;
}


export function loadFirstDomain() {
  var result = loadElements("/domains");
  var json = result.json();
  var domain = json[0];
  check(domain, {
    "Domain found": (r) => typeof domain != "undefined",
  });
  return domain;
}

export function loadControls(unitId, subType) {
  return loadElementsPaged("/controls", unitId, 10, 0, subType)
}

export function loadScenarios(unitId, subType) {
  return loadElementsPaged("/scenarios", unitId, 10, 0, subType)
}

export function loadPersons(unitId, subType) {
  return loadElementsPaged("/persons", unitId, 10, 0, subType)
}

export function loadAssets(unitId, subType) {
  return loadElementsPaged("/assets", unitId, 10, 0, subType)
}

export function loadDocuments(unitId, subType) {
  return loadElementsPaged("/documents", unitId, 10, 0, subType)
}

export function loadScopes(unitId, subType) {
  return loadElementsPaged("/scopes", unitId, 10, 0, subType)
}

export function loadProcesses(unitId, subType, status) {
  return loadElementsPaged("/processes", unitId, 10, 0, subType, status)
}

export function loadProcessingActivitiesByStatus(unitId)
{
  loadProcesses(unitId, "PRO_DataProcessing", "NEW");
  loadProcesses(unitId, "PRO_DataProcessing", "IN_PROGRESS");
  loadProcesses(unitId, "PRO_DataProcessing", "FOR_REVIEW");
  loadProcesses(unitId, "PRO_DataProcessing", "RELEASED");
  loadProcesses(unitId, "PRO_DataProcessing", "ARCHIVED");
}

export function loadElementsPaged(typeUrl, unitId, size, page, subType, status) {
  var url = new URL(VEO_BASE_URL + typeUrl);
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'GET ' + typeUrl 
    },
    timeout: REQUEST_TIMEOUT
  };
  if(!(status===undefined)) {
    url.searchParams.append("status", status );
  }
  url.searchParams.append("unit", unitId );
  if(!(subType===undefined)) {
    url.searchParams.append("subType", subType );
  }
  url.searchParams.append("size", size );
  url.searchParams.append("page", page );
  var result = http.get(url.toString(), params);

  check(result, {
    "Get processes is status 200": (r) => r.status === 200
  });
  if (result.status != 200) {
    console.error("GET processes status: " + result.status);
    console.error("GET processes URL: " + url);
  }

  return result;
}

export function loadTranslations() {
  loadElementsAndSleep("/translations?languages=de,en", 0);
}

export function loadForms() {
  var result = loadFormElementsAndSleep("/?domainId=" + domainId, 0);

  var json = result.json();
  check(result, {
    "Get forms returned a form": (r) => json.length > 0
  });

  return result;
}

export function loadReports() {
  var url = new URL(VEO_REPORTS_BASE_URL + "/reports");
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    timeout: REQUEST_TIMEOUT
  };
  var result = http.get(url.toString(), params);

  check(result, {
    "Get reports result is status 200": (r) => r.status === 200
  });
  if (result.status != 200) {
    console.error("GET reports status: " + result.status);
    console.error("GET reports URL: " + url);
  }

  return result;
}

export function loadHistory(unitId) {
  var url = new URL(VEO_HISTORY_BASE_URL + "/revisions/my-latest");
  var tag = "/history";
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'GET ' + tag 
    },
    timeout: REQUEST_TIMEOUT
  };
  url.searchParams.append("owner", "/units/" + unitId);
  var result = http.get(url.toString(), params);

  check(result, {
    "Get history result is status 200": (r) => r.status === 200
  });
  if (result.status != 200) {
    console.error("GET history status: " + result.status);
    console.error("GET history URL: " + url);
  }

  var json = result.json();
  check(result, {
    "Get history returned an entry": (r) => json.length > 0
  });

  return result;
}

export function loadCatalogs() {
  check(domainId, {
    "Domain ID is a valid UUID": (id) => checkIfValidUUID(id),
  });

  if(!checkIfValidUUID(domainId)) {
    console.error("Domain ID is not a valid UUID: " + domainId);
  }

  var url = new URL(VEO_BASE_URL + "/catalogs");
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    timeout: REQUEST_TIMEOUT
  };
  url.searchParams.append('domain', domainId);
  var result = http.get(url.toString(), params);

  check(result, {
    "Get catalogs result is status 200": (r) => r.status === 200
  });
  if (result.status != 200) {
    console.error("GET catalogs status: " + result.status);
    console.error("GET catalogs URL: " + url);
  }

  var json = result.json();
  check(result, {
    "Get catalogs returned a catalog": (r) => json.length > 0
  });

  return result;
}

export function loadSchema(type) {
  var url = new URL(VEO_BASE_URL + "/schemas/" + type);
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'GET /schemas/' + type 
    },
    timeout: REQUEST_TIMEOUT
  };
  url.searchParams.append('domains', domainId);
  var result = http.get(url.toString(), params);
  check(result, {
    "Get schema is status 200": (r) => r.status === 200
  });
  if (result.status != 200) {
    console.error("GET schema status: " + result.status);
    console.error("GET schema URL: " + url);
  }
  return result;
}

export function searchUnit(name) {
  var result = loadElementsAndSleep("/units", 0);
  var json = result.json();
  var unitWithName;
  for(var i = 0; i < json.length; i++) {
    var unit = json[i];
    if(unit.name == name) {
      unitWithName = unit;
      break;
    }
  }
  return unitWithName;
}

export function loadElements(path) {
  // console.log("loadElements( " + path + ")");
  return loadElementsWithBaseUrlAndSleep(VEO_BASE_URL, path, path, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadFormElements(path) {
  return loadElementsWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, "/forms", Math.random() * MAX_SLEEP_SECONDS);
}

export function loadElementsAndSleep(path, sleepSeconds) {
  // console.log("loadElementsAndSleep( " + path + ", " + sleepSeconds + ")");
  return loadElementsWithBaseUrlAndSleep(VEO_BASE_URL, path, path, sleepSeconds);
}

export function loadFormElementsAndSleep(path, sleepSeconds) {
  return loadElementsWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, "/forms", sleepSeconds);
}
export function loadElementsWithBaseUrlAndSleep(baseUrl, path, tag, sleepSeconds) {

  var url = baseUrl + path;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'GET ' + tag 
    },
    timeout: REQUEST_TIMEOUT
  };
  var result = http.get(url, params);

  check(result, {
    "Get forms result is status 200": (r) => r.status === 200,
  });
  if (result.status != 200) {
    console.error("GET status: " + result.status);
    console.error("GET URL: " + url);
  }
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
  return result;
}

export function loadElement(path, uuid) {
  return loadElementWithBaseUrlAndSleep(VEO_BASE_URL, path, uuid, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadFormElement(path, uuid) {
  return loadElementWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, uuid, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadElementWithBaseUrl(baseUrl, path, uuid) {
  return loadElementWithBaseUrlAndSleep(baseUrl, path, uuid, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadElementAndSleep(path, uuid, sleepSeconds) {
  loadElementWithBaseUrlAndSleep(VEO_BASE_URL, path, uuid, sleepSeconds)
}

export function loadFormElementAndSleep(path, uuid, sleepSeconds) {
  loadElementWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, uuid, sleepSeconds)
}

export function loadElementWithBaseUrlAndSleep(baseUrl, path, uuid, sleepSeconds) {
  check(uuid, {
    "Domain ID is a valid UUID": (id) => checkIfValidUUID(id),
  });
  if(!checkIfValidUUID(uuid)) {
    console.error("Not a valid UUID: " + uuid);
  }

  var url = baseUrl + path + uuid;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'GET ' + path 
    },
    timeout: REQUEST_TIMEOUT 
  };
  var result = http.get(url, params);

  check(result, {
    "Get unit result is status 200": (r) => r.status === 200
  });

  if (result.status != 200) {
    console.error("GET element status: " + result.status);
    console.error("GET element URL: " + url);
  }

  if(path != "/domains") {
    check(result, {
      "Etag is returned": (r) => r.headers["Etag"].length > 0,
    });
  }
  
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
  return result;
}

export function deleteElements(path, ids) {
  for(let id of ids) {
    deleteElement(path, id);
  }
}

function deleteElement(path, uuid) {
  var url = VEO_BASE_URL + path + uuid;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
    tags: { 
      name: 'DELETE ' + path 
    },
    timeout: REQUEST_TIMEOUT
  };
  var result = http.del(url, "", params);

  console.info("DEL element " + path + uuid);
  check(result, {
    "Delete element result is status 204": (result) => result.status === 204,
  });
  if (result.status != 204) {
    console.error("DEL element status: " + result.status);
    console.error("DEL element URL: " + url);
  }
}

export function getToken() {
  console.log("Getting token...");
  var url = KEYCLOAK_BASE_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";
  var params = {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    tags: { 
      name: 'POST /auth' 
    },
    timeout: REQUEST_TIMEOUT
  };
  let body = { 
    username:USER_NAME, 
    password:PASSWORD,
    grant_type:"password",
    client_id:KEYCLOAK_CLIENT_ID
  };
  var result = http.post(url, body, params);

  check(result, {
    "Get token result is status 200": (r) => r.status === 200,
  });
  if (result.status != 200) {
    console.error("POST auth token status: " + result.status_text);
    console.error("POST auth token URL: " + url);
  }
  TOKEN = "Bearer " + result.json().access_token;
}

function checkIfValidUUID(str) {
  // Regular expression to check if string is a valid UUID
  const regexExp = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/gi;
  return regexExp.test(str);
}

function getRandom(array) {
  return array[getRandomInt(array.length)];
}

function getRandomInt(max) {
  return Math.floor(Math.random() * max);
}