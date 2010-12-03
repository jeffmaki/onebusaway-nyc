//Copyright 2010, OpenPlans
//Licensed under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at

//http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

var OBA = window.OBA || {};

OBA.Config = {
		debug: true,

		// urls to fetch various data
		searchUrl: "search.action",
		routeShapeUrl: "/onebusaway-api-webapp/api/where/stops-for-route",
		stopsUrl: "/onebusaway-api-webapp/api/where/stops-for-location.json",
		stopUrl: "/onebusaway-api-webapp/api/where/arrivals-and-departures-for-stop",
		vehiclesUrl:"/onebusaway-api-webapp/api/where/trips-for-route",
		vehicleUrl: "/onebusaway-api-webapp/api/where/trip-for-vehicle",

		// milliseconds to wait in between polls for bus locations
		pollingInterval: 5000,

		// marker images used
		// (the path below needs to be absolute)
		vehicleIconFilePrefix: "/img/vehicle/vehicle",
		vehicleIconFileType: "png",
		vehicleIconSize: new google.maps.Size(20,20),
		vehicleIconCenter: new google.maps.Point(10,10),

		// (the path below needs to be absolute)
		stopIconFilePrefix: "/img/stop/stop",
		stopIconFileType: "png",
		stopIconSize: new google.maps.Size(24,24),
		stopIconCenter: new google.maps.Point(12,12),

		// api key used for webapp
		apiKey: "TEST",

		vehicleFilterFunction: function(type, tripStatus) {
			OBA.Util.log("POTENTIAL BUS: VEHICLE_ID=" + tripStatus.vehicleId + " DAT=" + tripStatus.distanceAlongTrip + " PREDICTED=" + tripStatus.predicted);
			
			// don't show non-realtime trips (row 8)
			if(tripStatus === null || tripStatus.predicted === false || tripStatus.distanceAlongTrip === 0) {
				OBA.Util.log("HIDING: STATE 1");
				return false;
			}
			
			var status = ((typeof tripStatus.status !== 'undefined' && tripStatus.status !== '') ? tripStatus.status : null);
			var phase = ((typeof tripStatus.phase !== 'undefined' && tripStatus.phase !== '') ? tripStatus.phase : null);

			OBA.Util.log("BUS: PREDICTED=" + tripStatus.predicted + " STATUS=" + status + " PHASE=" + phase);

			// hide disabled vehicles (row 7)
			if(status !== null && status.toLowerCase() === 'disabled') {
				OBA.Util.log("HIDING: STATE 2");
				return false;
			}

			// hide deviated vehicles in stop popup
			if(type === "stop" && status !== null && status.toLowerCase() === 'deviated') {
				OBA.Util.log("HIDING: STATE 3");
				return false;
			}
			
			// hide deadheading vehicles (row 3)
			// hide vehicles at the depot (row 1)
			if(phase !== null && phase.toLowerCase() !== 'in_progress') {
				OBA.Util.log("HIDING: STATE 4");
				return false;
			}
			
			// hide data >= hideTimeout seconds old (row 5)
			if(typeof tripStatus.lastUpdateTime !== 'undefined' && new Date().getTime() - tripStatus.lastUpdateTime >= 1000 * OBA.Config.hideTimeout) {
				OBA.Util.log("HIDING: STATE 5");
				return false;
			}

			OBA.Util.log("SHOWING");
			return true;
		},
		
		infoBubbleFooterFunction: function(type, query) {
			var html = '';
			
			html += '<div class="footer">';

			html += '<p><strong>At the bus stop...</strong></p>';
			
			html += '<p>';

			if(type === "stop")	{
				html += 'Text "MTA ' + query + '" to 41411 or ';
			}
			
			html += 'Visit <a href="http://' + window.location.hostname + '/m/?q=' + query + '">http://' + window.location.hostname + '/m/?q=' + query + '</a> on your smartphone!';
			html += '</p>';
			
			html += '</div>';
			
			return html;
		}
};
