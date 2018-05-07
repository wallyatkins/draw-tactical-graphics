
// Initialize Variables
var path, ink, scores;
var timer = 0, lastTimestamp = 0, lastTimestamp_check = 0, idx_guess = 0;
var d_scores = {};

var index = 0;

// Unit icon information
var fontInfo = this.getFontInfo("Arial",10,"bold");
var modifiers = {"SIZE":100};
var iconsToDraw = [
  {'type': 'Infantry Unit', 'code': 'SFGPUCI--------'},
  {'type': 'Engineering Unit', 'code': 'SFGPUCE--------'},
  {'type': 'Field Artillery Unit', 'code': 'SFGPUCF--------'},
  {'type': 'Reconnaissance Unit', 'code': 'SFGPUCR--------'},
  {'type': 'CBRN Unit', 'code': 'SFGPUUA--------'},
  {'type': 'Signal Unit', 'code': 'SFGPUUS--------'},
  {'type': 'Medical Unit', 'code': 'SFGPUSM--------'},
  {'type': 'Maintenance Unit', 'code': 'SFGPUSX--------'},
];

var worker = new Worker('milsym/SPWorker.js');

// Install Paper.js
paper.install(window);

// Initialize...
window.onload = function() {

  showModal();

  // Close the modal
  var span = document.getElementsByClassName('close')[0];
  span.onclick = function() {
    modal.style.display = 'none';
  }

  initInk();              // Initialize Ink array ()
  paper.setup('canvas');  // Setup Paper #canvas
  var tool = new Tool();  // Inititalize Paper Tool

  // Paper Tool Mouse Down Event
  tool.onMouseDown = function(event) {
    // New Paper Path and Settings
    path = new Path();
    path.strokeColor = 'black';
    path.strokeWidth = 10;

    // Get Time [ms] for each Guess (needed for accurate Google AI Guessing)
    var thisTimestamp = event.event.timeStamp;
    if(timer === 0){
      timer = 1;
      var time = 0;
    }else{
      var timeDelta = thisTimestamp - lastTimestamp;
      var time = ink[2][ink[2].length-1] + timeDelta;
    }

    // Get XY point from event w/ time [ms] to update Ink Array
    updateInk(event.point, time);
    // Draw XY point to Paper Path
    path.add(event.point);

    // Reset Timestamps
    lastTimestamp = thisTimestamp;
  }

  // Paper Tool Mouse Drag Event
  tool.onMouseDrag = function(event) {
    // Get Event Timestamp and Timestamp Delta
    var thisTimestamp = event.event.timeStamp ;
    var timeDelta = thisTimestamp - lastTimestamp;
    // Get new Time for Ink Array
    var time = ink[2][ink[2].length-1] + timeDelta;

    // Get XY point from event w/ time [ms] to update Ink Array
    updateInk(event.point, time);
    // Draw XY point to Paper Path
    path.add(event.point);

    // Reset Timestamps
    lastTimestamp = thisTimestamp;

    // Check Google AI Quickdraw every 250 m/s
    if(thisTimestamp - lastTimestamp_check > 250){
      //checkQuickDraw();
      lastTimestamp_check = thisTimestamp;
    }
  }
}

// Hide the modal stuff
window.onclick = function(event) {
  if (event.target == modal) {
    modal.style.display = 'none';
  }
}

// Unit font modifiers info
function getFontInfo(name, size, style){
    _ModifierFontName = name;
    _ModifierFontSize = size;
    if(style !== 'bold' || style !== 'normal')
    {
        _ModifierFontStyle = style;
    }
    else
    {
        _ModifierFontStyle = 'bold';
    }
    _ModifierFont = style + " " + size + "pt " + name;

    var measurements = armyc2.c2sd.renderer.utilities.RendererUtilities.measureFont(_ModifierFont);
    return {name: name, size:size, style:style, measurements:measurements};
}

// Initialize Ink Array
function initInk(){
  ink = [[],[],[]];
}

// Clear Paper Drawing Canvas
function clearDrawing() {

  // Remove Paper Path Layer
  paper.project.activeLayer.removeChildren();
  paper.view.draw();

  // Init Ink Array
  initInk();

  // Resert Variables
  timer = 0;
  idx_guess = 0;
  d_scores = {};
}

// Update Ink Array w/ XY Point + Time
function updateInk(point, time){
  ink[0].push(point.x);
  ink[1].push(point.y);
  ink[2].push(time);
}

// Get Paper Canvas Dimensions Width/Height
function getCanvasDimensions(){
  var w = document.getElementById('canvas').offsetWidth;
  var h = document.getElementById('canvas').offsetHeight;
  return {height: h, width: w};
}

// Check Quickdraw Google AI API
function postDrawing(){

  // Get Paper Canvas Weight/Height
  var c_dims = getCanvasDimensions();

  // Set Base URL to send drawings
  var url = '/drawing'

  // Set HTTP Headers
  var headers = {
    'Accept': '*/*',
    'Content-Type': 'application/json'
  };

  // Init HTTP Request
  var xhr = new XMLHttpRequest();
  xhr.open('POST', url);
  Object.keys(headers).forEach(function(key,index) {
      xhr.setRequestHeader(key, headers[key]);
  });

  // HTTP Request On Load
  xhr.onload = function() {
    if (xhr.status === 200) {
      res = xhr.responseText; // HTTP Response Text
      //parseResponse(res);     // Parse Response
      idx_guess += 1;         // Iterate Guess Index
    }
    else if (xhr.status !== 200) {
      console.log('Request failed.  Returned status of ' + xhr.status);
    }
  };

  // Create New Data Payload for Quickdraw Google AI API
  /* var data = {
    'input_type':0,
    'requests':[
      {
        'type':iconsToDraw[index].type,
        'code':iconsToDraw[index].code,
        'writing_guide':{'width': c_dims.width, "height":c_dims.height},
        'ink': [ink]
      }
    ]
  }; */

  var data = {'writing_guide':{'width': c_dims.width, "height":c_dims.height},
    'type':iconsToDraw[index].type,
    'code':iconsToDraw[index].code,
    'writing_guide':{'width': c_dims.width, "height":c_dims.height},
    // 'png':document.getElementById('canvas').toDataURL("image/png")
    'ink':paper.project.exportJSON()
  };

  // Convert Data Payload to JSON String
  var request_data = JSON.stringify(data);

  //alert(request_data);

  // Send HTTP Request w/ Data Payload
  xhr.send(request_data);

  // Reset the canvas and move to the next thing to drawing
  clearDrawing();
  index++;
  showModal();
}

// Parse Quickdraw Google AI API Response
function parseResponse(res){
  // Convert Response String to JSON
  var res_j = JSON.parse(res);
  // Extract Guess Score String from Response and Convert to JSON
  scores = JSON.parse(res_j[1][0][3].debug_info.match(/SCORESINKS: (.+) Combiner:/)[1]);
  // Add New Guess Scores to Score History
  updateScoresHistory();
}

function showModal(){

  if (index === iconsToDraw.length) {
    location.reload();
  }

  var modal = document.getElementById('modal');

  // Show the modal with the unit icon
  document.getElementById('instructions').innerHTML = 'Draw an ' + iconsToDraw[index].type;
  var data = {};
  data.id = "ID";
  data.symbolID = iconsToDraw[index].code;
  data.modifiers = modifiers;
  data.fontInfo = fontInfo;

  worker.onerror = function(error){
    armyc2.c2sd.renderer.utilities.ErrorLogger.LogException("SPWorker", "postMessage", error);
  };
  worker.onmessage = function(e){
    if(e.data.error){
      document.getElementById("unit").innerHTML = e.data.result;
      if(e.data.stack !== null){
        document.getElementById("unit").innerHTML = ("<br/>at " + e.data.stack);
      }
    }else{
      var svg = e.data.svg;
      document.getElementById("unit").innerHTML = svg;
    }
  }
  worker.postMessage(data);

  modal.style.display = 'block';
}

// Update Score History
function updateScoresHistory(){
  // Init Current Guesses Array
  var current_guesses = [];
  // Loop Through Each Score in Current List of Scores
  for(ii=0; ii<scores.length; ii++){
    // Get Guess+Score
    var guess = scores[ii][0];
    var score = scores[ii][1];

    // Add Guess+Score to Current Guess Array
    current_guesses.push(guess)

    // If Guess is in Data Scores (keys)...
    if(guess in d_scores){
      // Add Score to Data Scores
      d_scores[guess].push(score);
    // If Guess is Not In Data Scores...
    }else{
      // Init New Guess Index Array for Guess and Add to Score
      d_scores[guess] = createArray(idx_guess+1, null);
      d_scores[guess][idx_guess] = score;
    }
  }

  // Loop through Guesses in Data Scores
  for(guess in d_scores){
    // If Guess Not in Current Guesses Array...
    if(current_guesses.indexOf(guess) == -1){
      // Add Null Guess to Array
      d_scores[guess].push(null);
    }
  }

}

// Create and Fill Array
function createArray(len, itm) {
    var arr1 = [itm],
        arr2 = [];
    while (len > 0) {
        if (len & 1) arr2 = arr2.concat(arr1);
        arr1 = arr1.concat(arr1);
        len >>>= 1;
    }
    return arr2;
}
