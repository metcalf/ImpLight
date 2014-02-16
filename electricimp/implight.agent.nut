const TICKS_PER_SEC = 1000;
const MAX_TIME = 3600000;
const MAX_LEVEL = 255;

currentLevels <- [0.0, 0.0, 0.0];

// Pulled from http://jsfiddle.net/5hx6u/2/
const html = @"<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv=""content-type"" content=""text/html; charset=UTF-8"">
    <title>ImpLight</title>

    <link rel=""stylesheet"" type=""text/css"" href=""//netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css"">
  </head>
  <body>
    <div class=""container"" style=""margin-top:20px"">
      <form class=""form-horizontal"" action="""" method=""post"">
        <div class=""form-group"">
          <div class=""col-xs-2 text-right""><strong>Color</strong>
            <br/><strong class=""text-muted""><small>(required)</small></strong>
            <br/><small class=""text-muted"">0-255</small></div>
          <div class=""col-xs-2"">
            <label for=""rInput"">Red</label>
            <input type=""text"" id=""rInput"" name=""r"" class=""form-control"" value=""%d"" />
          </div>
          <div class=""col-xs-2"">
            <label for=""gInput"">Green</label>
            <input type=""text"" id=""gInput"" name=""g"" class=""form-control"" value=""%d"" />
          </div>
          <div class=""col-xs-2"">
            <label for=""bInput"">Blue</label>
            <input type=""text"" id=""bInput"" name=""b"" class=""form-control"" value=""%d"" />
          </div>
        </div>
        <hr/>
        <div class=""form-group"">
          <label for=""time"" class=""col-xs-2 control-label"">Dim Time
            <br/><small class=""text-muted"">(optional)</small></label>
          <div class=""col-xs-2"">
            <input type=""text"" id=""time"" name=""time"" class=""form-control"" value=""0"" />
          </div>
        </div>
        <hr/>
        <div class=""form-group"">
          <div class=""col-xs-offset-2 col-xs-10"">
            <button type=""submit"" class=""btn btn-default"">Set</button>
          </div>
        </div>
      </form>
    </div>
  </body>
</html>
"

class RequestError {
    constructor(code_, message_){
        code = code_;
        message = message_;
    }

    function WriteResponse(res){
        res.send(code, message)
    }

    code = 500;
    message = "Server error";
}

function toValidInt(key, data, min, max){
    local intVal;
    local prefix = format("Invalid request. Parameter `%s` must be an integer " +
        "between %d and %d but got: ", key, min, max);
    try {
        intVal = data[key].tointeger();
    } catch(exp){
        throw RequestError(400, prefix + data[key]);
    }

    if(intVal < min || intVal > max){
        throw RequestError(400, prefix + intVal);
    }

    return intVal;
}

function setDevice(time, levels){
    device.send("set", {
        "time": time,
        "levels": levels
    });
    // Optimistically update our local state
    currentLevels = clone levels;
}

function handleLightPost(req, res){
    local reqData = http.urldecode(req.body);
    local time = 0;
    local levels = [0.0, 0.0, 0.0];

    foreach(idx,key in ["r", "g", "b"]){
        if(!(key in reqData)){
            throw RequestError(400, "Invalid request. Must contain `" + key + "`");
        }

        levels[idx] = toValidInt(key, reqData, 0, MAX_LEVEL) / MAX_LEVEL.tofloat();
    }

    if("time" in reqData){
        time = toValidInt("time", reqData, 0, MAX_TIME) / TICKS_PER_SEC.tofloat();
    }

    setDevice(time, levels);

    handleLightGet(req, res)
}

function handleLightGet(req, res){
    local levels = []
    foreach(idx,level in currentLevels){
        levels.push(math.floor(level * MAX_LEVEL + 0.5));
    }

    local status;
    if(device.isconnected()){
        status = "connected";
    } else {
        status = "disconnected";
    }

    if("accept" in  req.headers && req.headers.accept == "application/json"){
        res.send(200, http.jsonencode({
            "status": status,
            "levels": {
                "r": levels[0],
                "g": levels[1],
                "b": levels[2],
            }
        }));
    } else {
        res.send(200, format(html, levels[0], levels[1], levels[2]));
    }
}

function route(req, res){
    if(req.path == "/light"){
        if(req.method == "GET"){
            handleLightGet(req, res);
        } else if (req.method == "POST") {
            handleLightPost(req, res);
        } else {
            throw RequestError(405, "Method not allowed.");
        }
    } else if(req.path == "/code") {
        if(req.method == "POST"){
            device.send("code", req.body);
            res.send(200, "OK");
        } else {
            throw RequestError(405, "Method not allowed.");
        }
    } else {
        throw RequestError(404, "Resource not found");
    }
}

http.onrequest(function(req, res){
    try {
        route(req, res);
    } catch(exp) {
        if(exp instanceof RequestError){
            server.log("RequestError (" + exp.code + "): " + exp.message);
            exp.WriteResponse(res);
        } else {
            throw exp;
        }
    }
});

device.on("status", function(status){
    foreach(idx,_ in currentLevels){
        currentLevels[idx] = status.levels[idx];
    }

    server.log(format("R:%.2f G:%.2f B:%.2f", currentLevels[0], currentLevels[1], currentLevels[2]));
});