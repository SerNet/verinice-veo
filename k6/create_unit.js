/*
Copyright (c) 2020 Daniel Murygin.

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

Before executing the script, these parameters must be set in the file:
 - BASE_URL - Base URL der veo API (default: https://veo.develop.verinice.com)
 - TOKEN - Token for Authorization

To run the script behind a proxy set environment variables before executing:

 export HTTPS_PROXY=http://cache.int.sernet.de:3128
 export HTTP_PROXY=http://cache.int.sernet.de:3128

After installing k6, this script is started with:

 k6 run create_unit.js

To run a load test with 10 virtual user (vus) for 30s, type:

 k6 run --vus 10 --duration 30s script.js

See k6 documentation for more options: 
https://k6.io/docs/getting-started/running-k6
*/
import { check } from "k6";
import { sleep } from "k6";
import { IFrameElement } from "k6/html";
import http from "k6/http";

// Base URL of the veo API
const BASE_URL = "https://veo.develop.verinice.com";
//const BASE_URL = "https://veo.staging.verinice.com";
//const BASE_URL = "http://localhost:8070";

// Token for Authorization
const TOKEN =
  "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRUEdsckZSVzlOUU5tTTNVZkVMNVM4SzRMT0tRNDdLcnk2elEwUmNaRTdzIn0.eyJleHAiOjE2MDU3MTc2MDAsImlhdCI6MTYwNTcxNTgwMCwiYXV0aF90aW1lIjoxNjA1NzA4MDIzLCJqdGkiOiIzMWMzOTcyYS0wNzQzLTQxMTctOTMzMi01N2VhYzExZGQ1OWEiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLnN0YWdpbmcudmVyaW5pY2UuY29tL2F1dGgvcmVhbG1zL3ZlcmluaWNlLXZlbyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI2NzI1MDM1OS1jZjUyLTQ4OTYtYTE3Yi1kNTNiYTkzNTYzYmUiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJ2ZW8tZGV2ZWxvcG1lbnQtY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6ImM5MDFhZjI0LWQ0ZTUtNDRmYy04OWM5LTM0OTZkYjJkMzQ3NiIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly92ZW8td2ViLmNwbXN5cy5pbyIsImh0dHBzOi8vdmVvLXdlYi5zdGFnaW5nLnZlcmluaWNlLmNvbSIsImh0dHBzOi8vdmVvLnN0YWdpbmcudmVyaW5pY2UuY29tIiwiKiIsImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCIsImh0dHBzOi8vdmVvLWZvcm1zLnN0YWdpbmcudmVyaW5pY2UuY29tIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ2ZW8tdXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCB2ZW8tdXNlciBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJEYW5pZWwgTXVyeWdpbiIsImdyb3VwcyI6WyIvdmVvX2NsaWVudDoyMTcxMjYwNC1lZDg1LTRmMDgtYWE0Ni0xY2YzOTYwN2VlOWUiLCIvdmVvLXVzZXIiXSwicHJlZmVycmVkX3VzZXJuYW1lIjoiZG0iLCJsb2NhbGUiOiJkZSIsImdpdmVuX25hbWUiOiJEYW5pZWwiLCJmYW1pbHlfbmFtZSI6Ik11cnlnaW4iLCJlbWFpbCI6ImRtQHNlcm5ldC5kZSJ9.siwSWP3YjHeQ5HY6mYjoQReL0Mwm8SofwyM9xkLPKykEy3V9NtEwVIjN4kd8mgUimHg3lp3eYeOFi78RC4vvYR95ebAeox5jJRLfTVmebrDta4B7hlTzARlh0qmwBgDrpo_0IiIP0VPggjTJ-_eZsyLJls5yAapDpB_hf4YJiEemmCaaAU2m07ew_NLD8xyKAeHkI78dJ2XmNdOQ8Q5ipxF0O54f_TgjxfRVmitKhb6vjyB8altpSBf0-u4xhXPMWtktTsDD2CCtyWbr89oKYVHKDGpKXtkEV9_jd8Dk2ZSVtkKelYymUjrdFPr8vFtVX5NRM5yg8xVTS4QOj3rrtw";

// Maximum number of seconds to sleep after a request
const MAX_SLEEP_SECONDS = 5;

export let options = {
  thresholds: {
    http_req_duration: ["p(99)<3000"], // 99% of requests must complete below 3s
    http_req_duration: ["p(95)<1000"], // 95% of requests must complete below 1s
    http_req_duration: ["p(80)<200"] // 70% of requests must complete below 200ms
  },
  stages: [
    { duration: "6m", target: 600, gracefulRampDown: '600s', gracefulStop: '600s' }, // Scale up, 6m to reach 600 VUS
    { duration: "4m", target: 600, gracefulRampDown: '600s', gracefulStop: '600s' }, // 4m with 600 VUS
    { duration: "10m", target: 0, gracefulRampDown: '600s', gracefulStop: '600s' } // Scale down, 10m from 600 to 0 VUS
  ],
};

// This method is executed for each virtual user
export default function () {
  loadElements("/units");

  var body = JSON.stringify({ name: "Unit created by k6" });
  var unitId = createElement("/units", body);

  var result = loadElement("/units/", unitId);
  var etag = extractTextBetweenQuotes(result.headers["Etag"]);

  body = JSON.stringify({ id: unitId, name: "New name of unit created by k6" });
  updateElement("/units/", unitId, body, etag);

  for (var i = 0; i < 10; i++) {
    var assetId = createAsset(unitId, i);
    loadElement("/assets/", assetId);
  }
  for (i = 0; i < 5; i++) {
    var processId = createProcess(unitId, i);
    loadElement("/processes/", processId);
  }

  deleteElement("/units/", unitId);
}

function createElement(path, body) {
  return createElementAndSleep(path, body, Math.random() * MAX_SLEEP_SECONDS);
}

function createElementAndSleep(path, body, sleepSeconds) {
  var url = BASE_URL + path;
  var params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: TOKEN,
    },
  };

  var result = http.post(url, body, params);
  check(result, {
    "Create element result is status 201": (r) => r.status === 201,
  });
  if (result.status != 201) {
    console.log("POST element status: " + result.status);
    console.log("POST element body:");
    console.log(JSON.stringify(body));
  }
  var elementId = result.json().resourceId;
  check(elementId, {
    "Element ID is returned": (r) => elementId.length == 36,
  });

  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
  return elementId;
}

function loadElements(path) {
  return loadElementsAndSleep(path, Math.random() * MAX_SLEEP_SECONDS);
}

function loadElementsAndSleep(path, sleepSeconds) {
  var url = BASE_URL + path;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
  };
  var result = http.get(url, params);

  check(result, {
    "Get units result is status 200": (r) => r.status === 200,
  });
  if (result.status != 200) {
    console.log("GET element status: " + result.status);
    console.log("GET element URL: " + url);
  }
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
  return result;
}

function loadElement(path, uuid) {
  return loadElementAndSleep(path, uuid, Math.random() * MAX_SLEEP_SECONDS);
}

function loadElementAndSleep(path, uuid, sleepSeconds) {
  var url = BASE_URL + path + uuid;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
  };
  var result = http.get(url, params);

  check(result, {
    "Get unit result is status 200": (r) => r.status === 200,
    "Etag is returned": (r) => r.headers["Etag"].length > 0,
  });
  if (result.status != 200) {
    console.log("GET element status: " + result.status);
    console.log("GET element URL: " + url);
  }
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
  return result;
}

function updateElement(path, uuid, body, etag) {
  updateElementAndSleep(
    path,
    uuid,
    body,
    etag,
    Math.random() * MAX_SLEEP_SECONDS
  );
}

function updateElementAndSleep(path, uuid, body, etag, sleepSeconds) {
  var url = BASE_URL + path + uuid;
  var params = {
    headers: {
      "Content-Type": "application/json",
      "If-Match": etag,
      Authorization: TOKEN,
    },
  };
  var result = http.put(url, body, params);
  check(result, {
    "Update element result is status 200": (r) => r.status === 200,
  });
  if (result.status != 200) {
    console.log("PUT element status: " + result.status);
    console.log("PUT element body:");
    console.log(JSON.stringify(body));
  }
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
}

function deleteElement(path, uuid) {
  deleteElementAndSleep(path, uuid, Math.random() * MAX_SLEEP_SECONDS);
}

function deleteElementAndSleep(path, uuid, sleepSeconds) {
  var url = BASE_URL + path + uuid;
  var params = {
    headers: {
      Authorization: TOKEN,
    },
  };
  var result = http.del(url, "", params);

  check(result, {
    "Delete unit result is status 204": (r) => r.status === 204,
  });
  if (result.status != 204) {
    console.log("DEL element status: " + result.status);
    console.log("DEL element URL: " + url);
  }
  if (sleepSeconds > 0) {
    sleep(sleepSeconds);
  }
}

function extractTextBetweenQuotes(str) {
  const matches = str.match(/"(.*?)"/);
  return matches ? matches[1] : str;
}

function createAsset(unitId, nr) {
  var body = JSON.stringify({
    name: "Asset " + nr + " created by k6",
    owner: {
      displayName: "Unit created by k6",
      targetUri: BASE_URL + "/units/" + unitId,
    },
  });
  return createElement("/assets", body);
}

function createProcess(unitId, nr) {
  var body = JSON.stringify({
    name: "Verarbeitungstätigkeit " + nr,
    customAspects: {
      process_AccessAuthorization: {
        type: "process_AccessAuthorization",
        attributes: {
          process_AccessAuthorization_authorizationConcept: true,
          process_AccessAuthorization_description:
            "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. ",
        },
      },
      process_JointControllership: {
        type: "process_JointControllership",
        attributes: {
          process_JointControllership_jointResponsiblePersons: true,
        },
      },
      process_InformationObligations: {
        type: "process_InformationObligations",
        attributes: {
          process_InformationObligations_explanation:
            "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. ",
          process_InformationObligations_document: "http://sernet.de",
        },
      },
      process_ProcessingDetails: {
        type: "process_ProcessingDetails",
        attributes: {
          process_ProcessingDetails_comment:
            "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat. ",
          process_ProcessingDetails_operationalStage:
            "process_ProcessingDetails_operationalStage_test",
          process_ProcessingDetails_typeOfSurvey:
            "process_ProcessingDetails_typeOfSurvey_newsurvey",
        },
      },
      process_GeneralInformation: {
        type: "process_GeneralInformation",
        attributes: {
          process_GeneralInformation_document: "https://v.de",
        },
      },
      process_SensitiveData: {
        type: "process_SensitiveData",
        attributes: {
          process_SensitiveData_comment:
            "Auch gibt es niemanden, der den Schmerz an sich liebt, sucht oder wünscht, nur, weil er Schmerz ist, es sei denn, es kommt zu zufälligen Umständen, in denen Mühen und Schmerz ihm große Freude bereiten können.",
        },
      },
    },
    abbreviation: "VT1",
    description: "Lorem ipsum dolor sit amet.",
    owner: {
      displayName: "Unit created by k6",
      targetUri: BASE_URL + "/units/" + unitId,
    },
  });

  return createElement("/processes", body);
}
