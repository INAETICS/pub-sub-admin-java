/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
/*! ineaticsDashboard.js - functions for rendering statistics - Copyright (C) 2015 - INAETICS */
"use strict";

var interval = 60000 // ms

function getAndShowNodes(){
	getJSON("/hosts", showNodes, 0)
}



var values = [];
var ips = [];

function showNodes(json){
	console.log("showNodes");
	var tbody = document.getElementById('nodes')

	while (tbody.firstChild) {
		tbody.removeChild(tbody.firstChild);
	}
	
	for (var index = 0; index < json.length; ++index) {
		(function(){
		var ip = json[index];
		ips.push(ip);
		
		
		console.log("showNodes "+ip);
		var f = document.createElement("div");
		
		var pubTitle = document.createElement("small");
		pubTitle.innerHTML = "IP: " + ip + " Publisher: ";
		var topic = document.createElement("input");
		topic.setAttribute('placeholder', 'Topic');
		
		var buttonStart = document.createElement("button");
		buttonStart.setAttribute('class', 'btn');
		buttonStart.innerHTML = 'Start';
		buttonStart.onclick = function(){postJSON("http://"+ip+":8080/publisher/start?topic="+topic.value);};
		
		var buttonStop = document.createElement("button");
		buttonStop.setAttribute('class', 'btn');
		buttonStop.innerHTML = 'Stop';
		buttonStop.onclick = function(){postJSON("http://"+ip+":8080/publisher/stop");};
		
		var publish = document.createElement("input");
		publish.setAttribute('placeholder', 'Value');
		publish.setAttribute('type', 'number');
		
		var buttonPublish = document.createElement("button");
		buttonPublish.setAttribute('class', 'btn');
		buttonPublish.innerHTML = 'Publish';
		buttonPublish.onclick = function(){postJSON("http://"+ip+":8080/publisher/publish?value="+publish.value);};
		
		var subTitle = document.createElement("small");
		subTitle.innerHTML =  " Subscriber: ";
		
		var subTopic = document.createElement("input");
		subTopic.setAttribute('placeholder', 'Topic');
		
		
		var buttonStartSub = document.createElement("button");
		buttonStartSub.setAttribute('class', 'btn');
		buttonStartSub.innerHTML = 'Start';
		buttonStartSub.onclick = function(){postJSON("http://"+ip+":8080/subscriber/start?topic="+subTopic.value);};
		
		var buttonStopSub = document.createElement("button");
		buttonStopSub.setAttribute('class', 'btn');
		buttonStopSub.innerHTML = 'Stop';
		buttonStopSub.onclick = function(){postJSON("http://"+ip+":8080/subscriber/stop");};
		
		var buttonStopSub = document.createElement("button");
		buttonStopSub.setAttribute('class', 'btn');
		buttonStopSub.innerHTML = 'Stop';
		buttonStopSub.onclick = function(){postJSON("http://"+ip+":8080/subscriber/stop");};
		
		var value = document.createElement("small");
		value.innerHTML =  " Values: ";
		values.push(value);
		f.appendChild(pubTitle);
		f.appendChild(topic);
		f.appendChild(buttonStart);
		f.appendChild(buttonStop);
		f.appendChild(publish);
		f.appendChild(buttonPublish);
		f.appendChild(subTitle);
		f.appendChild(subTopic);
		f.appendChild(buttonStartSub);
		f.appendChild(buttonStopSub);
		f.appendChild(value);
		nodes.appendChild(f);
		})();
	}
}
function refreshValues(){
	console.log("refreshValues");
	
	for (var index = 0; index < ips.length; ++index) {
		(function(ip, value){
			var xmlHttp = new XMLHttpRequest();
    		xmlHttp.onreadystatechange = function() { 
        		if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
            		value.innerHTML = "Values: "+  xmlHttp.responseText;
    		}
    		xmlHttp.open("GET", "http://"+ip+":8080/subscriber/getBuffer", true); // true for asynchronous 
    		xmlHttp.send(null);
		})(ips[index], values[index]);
	}
	
	setTimeout(refreshValues, 5000);
}

window.onload = function() {
	console.log("onload");
	getAndShowNodes()
	refreshValues();
}
