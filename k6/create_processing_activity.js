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

 -e name=<USER_NAME> required
 -e password=<PASSWORD> required
 -e host=<HOSTNAME> e.g. verinice.com
 -e keycloak_url=<KEYCLOAK_BASE_URL> e.g. https://auth.verinice.com
 -e keycloak_client=<KEYCLOAK_CLIENT_ID> e.g. veo-prod

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

const HOSTNAME = __ENV.host;
const USER_NAME = __ENV.name;
const PASSWORD = __ENV.password;
const KEYCLOAK_BASE_URL = __ENV.keycloak_url;
const KEYCLOAK_CLIENT_ID = __ENV.keycloak_client; 

// Base URLs of the APIs
const VEO_BASE_URL = "https://api." + HOSTNAME + "/veo";
const VEO_FORMS_BASE_URL = "https://api." + HOSTNAME + "/forms";
const VEO_HISTORY_BASE_URL = "https://api." + HOSTNAME + "/history";
const VEO_REPORTS_BASE_URL = "https://api." + HOSTNAME + "/reporting";

// Keycloak authentication parameter
const KEYCLOAK_REALM = "verinice-veo";

// Maximum number of seconds to sleep after a request
const MAX_SLEEP_SECONDS = 5;

const RESPONSIBLE_BODY = JSON.parse(open("./responsible_body.json"));
const JOINT_CONTROLLER = JSON.parse(open("./joint_controller.json"));
const PERSON = JSON.parse(open("./person.json"));
const TOM = JSON.parse(open("./tom.json"));
const DATA_TYPE = JSON.parse(open("./data_type.json"));
const DATA_TRANSFER = JSON.parse(open("./data_transfer.json"));
const IT_SYSTEM = JSON.parse(open("./it_system.json"));
const APPLICATION = JSON.parse(open("./application.json"));
const DATA_PROCESSING = JSON.parse(open("./data_processing.json"));

// Token for Authorization "Bearer <TOKEN>"
let TOKEN;

// ID of the unit being processed
let unitId;

// ID of the domain being processed
let domainId;


export let options = {
  thresholds: {
    http_req_duration: ["p(99)<3000"], // 99% of requests must complete below 3s
    http_req_duration: ["p(95)<1000"], // 95% of requests must complete below 1s
    http_req_duration: ["p(80)<200"] // 80% of requests must complete below 200ms
  },
  stages: [
    { duration: "3m", target: 100, gracefulRampDown: '600s', gracefulStop: '600s' }, // Scale up, 3m to reach 100 VUS
    { duration: "4m", target: 100, gracefulRampDown: '600s', gracefulStop: '600s' }, // 4m with 100 VUS
    { duration: "3m", target: 0, gracefulRampDown: '600s', gracefulStop: '600s' } // Scale down, 3m from 100 to 0 VUS
  ],
};

// This method is executed for each virtual user
export default function () {
  console.info("Starting iteration...");
  getToken();
  loadUnitSelection();
  loadDashboard();
  let responsibleBodyId = createResponsibleBody();
  let jointControllerId = createJointController();
  loadDashboard();
  let personId = createPerson();
  let tomId = createTOM();
  let dataTypeId = createDataType();
  DATA_TRANSFER.links.process_dataType[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + dataTypeId;
  let dataTransferId = createDataTransfer();
  loadDashboard();
  let itSystemId = createItSystem();
  let applicationId = createApplication();
  loadDashboard();
  DATA_PROCESSING.links.process_responsiblePerson[0].target.targetUri = "https://api." + HOSTNAME + "/veo/persons/" + personId;
  DATA_PROCESSING.links.process_responsibleBody[0].target.targetUri = "https://api." + HOSTNAME + "/veo/scopes/" + responsibleBodyId;
  DATA_PROCESSING.links.process_jointControllership[0].target.targetUri = "https://api." + HOSTNAME + "/veo/scopes/" + jointControllerId;
  DATA_PROCESSING.links.process_dataType[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + dataTypeId;
  DATA_PROCESSING.links.process_requiredApplications[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + applicationId;
  DATA_PROCESSING.links.process_requiredITSystems[0].target.targetUri = "https://api." + HOSTNAME + "/veo/assets/" + itSystemId;
  DATA_PROCESSING.links.process_dataTransmission[0].target.targetUri = "https://api." + HOSTNAME + "/veo/processes/" + dataTransferId;
  let dataProcessingId = createDataProcessing();
  

  deleteElement("/processes/", dataProcessingId);
  deleteElement("/processes/", dataTransferId);
  deleteElement("/scopes/", responsibleBodyId);
  deleteElement("/scopes/", jointControllerId);
  deleteElement("/persons/", personId);
  deleteElement("/controls/", tomId);
  deleteElement("/assets/", dataTypeId);
  deleteElement("/assets/", itSystemId);
  deleteElement("/assets/", applicationId);
}

export function loadUnitSelection() {
  console.info("Loading unit selection...");
  loadElementsAndSleep("/domains", 0);
  loadElementsAndSleep("/units", 0);
  loadElementsAndSleep("/domains", 0);
  loadElementsAndSleep("/units", 0);
  loadElementsAndSleep("/types", 0);

  var unitName = "Demo";


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
  loadElementAndSleep("/units/", unitId, 0);
  var domain = loadFirstDomain();
  domainId = domain.id;

  check(domainId, {
    "Domain ID is a valid UUID": (id) => checkIfValidUUID(id),
  });

  if(!checkIfValidUUID(domainId)) {
    console.error("Domain ID is not a valid UUID: " + domainId);
  }

  loadElementAndSleep("/domains/", domainId, 0);
  loadElementAndSleep("/units/", unitId, 0);
  loadForms();
  loadElementAndSleep("/domains/", domainId, 0);
  loadForms();
  loadCatalogs(domainId);
  loadForms();
  loadHistory(unitId);
  loadReports();
  loadProcesses(unitId, "PRO_DataProcessing");
  loadProcessingActivitiesByStatus(unitId);
  loadProcesses(unitId, "PRO_DataTransfer");
  loadScopes(unitId, "SCP_ResponsibleBody");
  loadDocuments(unitId, "DOC_Document");
  loadScopes(unitId, "SCP_Controller");
  loadAssets(unitId, "AST_Application");
  loadPersons(unitId, "PER_DataProtectionOfficer");
  loadScenarios(unitId, "SCN_Scenario");
  loadControls(unitId, "CTL_TOM");
  loadAssets(unitId, "AST_IT-System");
  loadPersons(unitId, "PER_Person");
  loadScopes(unitId, "SCP_JointController");
}

export function createDataProcessing() {
  console.info("Creating data processing...");
  loadForms();
  loadScopes(unitId, "PRO_DataProcessing");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("process");
  loadHistory(unitId);
  return createElement("/processes", DATA_PROCESSING, "PRO_DataProcessing",domainId,unitId);
}

export function createApplication() {
  console.info("Creating application...");
  loadForms();
  loadScopes(unitId, "AST_Application");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("asset");
  loadHistory(unitId);
  return createElement("/assets", APPLICATION, "AST_Application",domainId,unitId);
}

export function createItSystem() {
  console.info("Creating it system...");
  loadForms();
  loadScopes(unitId, "AST_IT-System");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("asset");
  loadHistory(unitId);
  return createElement("/assets", IT_SYSTEM, "AST_IT-System",domainId,unitId);
}

export function createDataTransfer() {
  console.info("Creating data transfer...");
  loadForms();
  loadScopes(unitId, "PRO_DataTransfer");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("process");
  loadHistory(unitId);
  return createElement("/processes", DATA_TRANSFER, "PRO_DataTransfer",domainId,unitId);
}

export function createDataType() {
  console.info("Creating data type...");
  loadForms();
  loadScopes(unitId, "AST_Datatype");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("asset");
  loadHistory(unitId);
  return createElement("/assets", DATA_TYPE, "AST_Datatype",domainId,unitId);
}

export function createTOM() {
  console.info("Creating TOM...");
  loadForms();
  loadScopes(unitId, "CTL_TOM");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("control");
  loadHistory(unitId);
  return createElement("/controls", TOM, "CTL_TOM",domainId,unitId);
}

export function createPerson() {
  console.info("Creating person...");
  loadForms();
  loadScopes(unitId, "PER_Person");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
  loadSchema("person");
  loadHistory(unitId);
  return createElement("/persons", PERSON, "PER_Person",domainId,unitId);
}

export function createResponsibleBody() {
  console.info("Creating responsible body...");
  loadForms();
  loadScopes(unitId, "SCP_ResponsibleBody");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
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
  return createElement("/scopes", RESPONSIBLE_BODY, "SCP_ResponsibleBody",domainId,unitId);
}

export function createJointController() {
  console.info("Creating joint controller...");
  loadForms();
  loadScopes(unitId, "SCP_JointController");
  sleep(Math.random() * MAX_SLEEP_SECONDS);
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
  return createElement("/scopes", JOINT_CONTROLLER, "SCP_JointController",domainId,unitId);
}

export function createElement(path, body, subType, domainId, unitId) {
  body.owner.targetUri = VEO_BASE_URL + "/units/" + unitId;
  body.domains[domainId] = {"subType":subType,"status":"NEW"};
  var url = VEO_BASE_URL + path;
  var params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: TOKEN,
    },
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

export function loadForms() {
  var result = loadFormElementsAndSleep("", 0);

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
  var params = {
    headers: {
      Authorization: TOKEN,
    },
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

export function loadCatalogs(domainId) {
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
  return loadElementsWithBaseUrlAndSleep(VEO_BASE_URL, path, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadFormElements(path) {
  return loadElementsWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, Math.random() * MAX_SLEEP_SECONDS);
}

export function loadElementsAndSleep(path, sleepSeconds) {
  // console.log("loadElementsAndSleep( " + path + ", " + sleepSeconds + ")");
  return loadElementsWithBaseUrlAndSleep(VEO_BASE_URL, path, sleepSeconds);
}

export function loadFormElementsAndSleep(path, sleepSeconds) {
  return loadElementsWithBaseUrlAndSleep(VEO_FORMS_BASE_URL, path, sleepSeconds);
}

export function loadElementsWithBaseUrlAndSleep(baseUrl, path, sleepSeconds) {
  var url = baseUrl + path;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
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
  };
  var result = http.del(url, "", params);

  console.info("DEL element " + path + " / " + uuid + ", status: " + result.status);
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
  var url = KEYCLOAK_BASE_URL + "/auth/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";
  var params = {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
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