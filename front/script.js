
var socket;
function connectToWs() {
    socket = new WebSocket("ws://localhost:8080/order");
    socket.onopen = function () {  alert("Connected"); };
    socket.onmessage = onMessageWs;
    socket.onclose = function(event) {
      if (event.wasClean) {
        alert(`Closed, code=${event.code}, reason=${event.reason}`);
      } else {
        alert('Interrupted');
      }
    };
    socket.onerror = function(error) {
      alert(`Error`);
    };
}

function onMessageWs(event) {
    var states = event.data.split(',');
    if (states.length == 1) {
        alert(`Current state: ${states[0]}`);
        changeBackground(states[0])
    } else if (states.length == 2) {
        alert(`Current state: ${states[1]}, previous state: ${states[0]}`);
        changeBackground(states[1], states[0])
    }
}

function changeBackground(current, prev) {
    var elements = document.getElementsByClassName("block")
            for (var i = 0; i < elements.length; i++) {
                elements[i].style.backgroundColor="#FFFFFF";
    }
    document.getElementById(current).style.backgroundColor="#008000"
    document.getElementById(prev).style.backgroundColor="#ffff00"
}

function confirmOrder() {
    socket.send("order-confirmation")
}

