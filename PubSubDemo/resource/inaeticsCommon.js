/*! ineaticsCommon.js - shared functions - Copyright (C) 2015, 2016 - INAETICS */

/*! getJSON - recursively returns the JSON result when querying an URL.
 * inputs: url, the URL to retrieve;
 *         callback, the function to call when a JSON result is 
 *           obtained. The function is given the JSON response;
 *         ival, the interval in milliseconds.
 * output: none. 
 */
function getJSON(url, callback, ival) {
	var xhr = new XMLHttpRequest()
	xhr.open('GET', url, true)
	xhr.responseType = 'json'
	xhr.onload = function(e) {
		try {
			if (this.status != 200) {
				console.log("failed to obtain " + url)
			} else {
				callback(this.response)
			}
		} finally {
			if (ival > 0) {
				setTimeout(function() { getJSON(url, callback, ival) }, ival)
			}
		}
	}
	xhr.send();
}

/*! postJSON - posts (form) data to an URL.
 * inputs: url, the URL to post;
 *         data, the FormData object to post.
 */
function postJSON(url, data) {
	var xhr = new XMLHttpRequest()
	xhr.open('POST', url, true)
	xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	xhr.send(data)
}

var chartWidth = 450;
var chartHeight = 350;

function fmtChartJSPerso(config, value, fmt) {
	if (fmt == "DateTime") {
		return new Date(value).toLocaleTimeString('nl-NL', { hour12: false })
	}
	return value;
}

function getChartOpts(stats) {
	var opts = { 
		canvasBorders: false,
		inGraphDataShow: false,
		responsive: true,
		animation: false,
		responsiveMinWidth: chartWidth,
		responsiveMaxWidth: chartWidth,
		responsiveMinHeight: chartHeight,
		responsiveMaxHeight: chartHeight,
	    graphTitleFontSize: 16,
		pointDot: false, 
		bezierCurve: false,
		rotateLabels: 60,
		datasetFill: true,
		yAxisUnitFontSize: 14,
		yAxisLabel: stats.type,
		yAxisUnit: stats.unit,
		fmtXLabel: "DateTime",
		graphTitle: stats.displayName
	};
	if (stats.type == "utilization") {
		opts.scaleOverride = true
		opts.scaleStartValue = 0
		opts.scaleSteps = 10
		opts.scaleStepWidth = 10
	}
	return opts;
}

function getData(stats) {
	return { 
		labels: stats.timestamps || [ 0 ], 
		datasets: [ {
			fillColor: "rgba(151,187,205,0.5)",
	        strokeColor: "rgba(151,187,205,1)",
			data: stats.values && stats.values.length > 0 ? stats.values : [ 0 ]
		} ] 
	}
}

String.prototype.capitalizeFirstLetter = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
}
