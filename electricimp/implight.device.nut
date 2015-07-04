const FREQUENCY = 500.0;
const DIM_STEPS = 1000;
const REPORT_INTERVAL = 0.25;
const VALUE_INCR = 0.05;
const COLOR_INCR = 0.05;
const DEFAULT_ON_LEVEL = 0.5;
const DIY_SET_PULSE = 0.05;

currentLevels <- [0.0, 0.0, 0.0];
lastOnLevels <- null;

startLevels <- [0.0, 0.0, 0.0];
targetLevels <- [0.0, 0.0, 0.0];
dimEndTime <- 0;
dimStartTime <- 0;
lastReport <- null;
timer <- null;
currDiy <- null;

permValues <- server.load();

irCodes <- {
    // Top row
    "3A": {"type": "valueChange", "incr": VALUE_INCR},
    "BA": {"type": "valueChange", "incr": -VALUE_INCR},
    // "82": {"type": "play"}
    "02": {"type": "power"},

    // Color keys
    "1A": {"type": "color", "hue": 19, "sat":1.000},
    "2A": {"type": "color", "hue": 35, "sat":1.000},
    "0A": {"type": "color", "hue": 40, "sat":1.000},
    "38": {"type": "color", "hue": 45, "sat":1.000},
    "18": {"type": "color", "hue": 52, "sat":1.000},
    "9A": {"type": "color", "hue": 100, "sat":0.958},
    "AA": {"type": "color", "hue": 146, "sat":0.625},
    "8A": {"type": "color", "hue": 152, "sat":0.625},
    "B8": {"type": "color", "hue": 159, "sat":0.625},
    "98": {"type": "color", "hue": 168, "sat":0.621},
    "A2": {"type": "color", "hue": 221, "sat":0.714},
    "92": {"type": "color", "hue": 241, "sat":0.487},
    "B2": {"type": "color", "hue": 251, "sat":0.487},
    "78": {"type": "color", "hue": 262, "sat":0.483},
    "58": {"type": "color", "hue": 278, "sat":0.483},
    "22": {"type": "color", "hue": 0, "sat":0.000},
    "12": {"type": "color", "hue": 278, "sat":0.227},
    "32": {"type": "color", "hue": 258, "sat":0.228},
    "F8": {"type": "color", "hue": 188, "sat":0.767},
    "D8": {"type": "color", "hue": 192, "sat":0.839},

    // Color control keys
    "28": {"type": "levelChange", "levels": [COLOR_INCR, 0, 0]},
    "08": {"type": "levelChange", "levels": [-COLOR_INCR, 0, 0]},
    "A8": {"type": "levelChange", "levels": [0, COLOR_INCR, 0]},
    "88": {"type": "levelChange", "levels": [0, -COLOR_INCR, 0]},
    "68": {"type": "levelChange", "levels": [0, 0, COLOR_INCR]},
    "48": {"type": "levelChange", "levels": [0, 0, -COLOR_INCR]},

    // DIY
    "30": {"type": "diy", "idx": 0},
    "B0": {"type": "diy", "idx": 1},
    "70": {"type": "diy", "idx": 2},
    "10": {"type": "diy", "idx": 3},
    "90": {"type": "diy", "idx": 4},
    "50": {"type": "diy", "idx": 5},
}

leds <- [hardware.pin1, hardware.pin5, hardware.pin7];
irRx <- hardware.pin2;

/*
 * Generic Class to learn IR Remote Control Codes
 * Useful for:
 * 		- TV Remotes
 *		- Air conditioner / heater units
 * 		- Fans / remote-control light fixtures
 *		- Other things not yet attempted!
 *
 * For more information on Differential Pulse Position Modulation, see
 * http://learn.adafruit.com/ir-sensor
 *
 */
class IR_receiver {
    /* Note that the receive loops runs at about 160 us per iteration */

	/* Receiver Thresholds in us. Inter-pulse times < THRESH_0 are zeros,
	 * while times > THRESH_0 but < THRESH_1 are ones, and times > THRESH_1
	 * are either the end of a pulse train or the start pulse at the beginning of a code */
	THRESH_0					= 1000;  // us (~5 iterations)
	THRESH_1					= 2000; // us (~11 iterations)

	/* IR Receive Timeouts
	 * IR_RX_DONE is the max time to wait after a pulse before determining that the
	 * pulse train is complete and stopping the reciever. */
	IR_RX_DONE					= 12000; // us

	/* The receiver is disabled between codes to prevent firing the callback multiple times (as
	 * most remotes send the code multiple times per button press). IR_RX_DISABLE determines how
	 * long the receiver is disabled after successfully receiving a code. */
	IR_RX_DISABLE				= 0.2500; // seconds

	/* The Vishay TSOP6238TT IR Receiver IC is active-low, while a simple IR detector circuit with a
	 * IR Phototransistor and resistor will be active-high. */
	IR_IDLE_STATE				= 1;

	rx_pin = null;

	/* Callback to trigger on receive */
	callback = null;

	/*
	 * Receive a new IR Code on the input pin.
	 *
	 * This function is configured as a state-change callback on the receive pin in the constructor,
	 * so it must be defined before the constructor.
	 */
	function receive() {

		// Code is stored as a string of 1's and 0's as the pulses are measured.
		local newcode = array(256);
		local index = 0;

        local state = 0; // dummy value; will be set again before being used
		local last_state = rx_pin.read();
		local duration = 0;

		local start = hardware.micros();
		local last_change_time = start;
		local now = start;

		local times = array(256);
		local timesIndex = 0;

		/*
		 * This loop runs much faster with while(1) than with a timeout check in the while condition
		 */
		while (1) {

			/* determine if pin has changed state since last read
			 * get a timestamp in case it has; we don't want to wait for code to execute before getting the
			 * timestamp, as this will make the reading less accurate. */
			state = rx_pin.read();
			now = hardware.micros();

			if (state == last_state) {
				// last state change was over IR_RX_DONE ago; we're done with code; quit.
				if ((now - last_change_time) > IR_RX_DONE) {
					break;
				} else {
					// no state change; go back to the top of the while loop and check again
					continue;
				}
			}

			// check and see if the variable (low) portion of the pulse has just ended
			if (state != IR_IDLE_STATE) {
				// the low time just ended. Measure it and add to the code string
				duration = now - last_change_time;

				if (duration < THRESH_0) {
					newcode[index++] = 0;
                    //times[timesIndex++] = duration;
				} else if (duration < THRESH_1) {
					newcode[index++] = 1;
                    //times[timesIndex++] = duration;
				}
			}

			last_state = state;
			last_change_time = now;

			// if we're here, we're currently measuring the low time of a pulse
			// just wait for the next state change and we'll tally it up
		}

        if(index == 0){
            return;
        }

		// codes are sent multiple times, so disable the receiver briefly before re-enabling
		disable();
		imp.wakeup(IR_RX_DISABLE, enable.bindenv(this));

		callback(newcode, index);
	}

	/*
	 * Instantiate a new IR Code Reciever
	 *
	 * Input:
	 * 		_rx_pin: (pin object) pin to listen to for codes.
	 *			Requires a pin that supports state-change callbacks.
	 * 		_rx_idle_state: (integer) 1 or 0. State of the RX Pin when idle (no code being transmitted).
	 * 		_callback: (function) function to call when a code is received.
	 *
	 * 		OPTIONAL:
	 *
	 * 		_thresh_0: (integer) threshold in microseconds for a "0". Inter-pulse gaps shorter than this will
	 * 			result in a zero being received.
	 *		_thresh_1: (integer) threshold in microseconds for a "1". Inter-pulse gaps longer than THRESH_0 but
	 * 			shorter than THRESH_1 will result in a 1 being received. Gaps longer than THRESH_1 are ignored.
	 *		_ir_rx_done: (integer) time in microseconds to wait for the next pulse before determining that the end
	 * 			of a pulse train has been reached.
	 * 	    _ir_rx_disable: (integer) time in seconds to disable the receiver after successfully receiving a code.
	 */
	constructor(_rx_pin, _rx_idle_state, _callback, _thresh_0 = null, _thresh_1 = null,
		_ir_rx_done = null, _ir_rx_disable = null) {
		this.rx_pin = _rx_pin;
		rx_pin.configure(DIGITAL_IN, receive.bindenv(this));

		IR_IDLE_STATE = _rx_idle_state;

		callback = _callback;

		/* If any of the timeouts were passed in as arguments, override the default value for that
		 * timeout here. */
		if (_thresh_0) {
			THRESH_0 = _thresh_0;
		}

		if (_thresh_1) {
			THRESH_1 = _thresh_1;
		}

		if (_ir_rx_done) {
			IR_RX_DONE = _ir_rx_done;
		}

		if (_ir_rx_disable) {
			IR_RX_DISABLE = _ir_rx_disable;
		}
	}

	function enable() {
		rx_pin.configure(DIGITAL_IN, receive.bindenv(this));
	}

	function disable() {
		rx_pin.configure(DIGITAL_IN);
	}
}


function rgbToHsv(rgb){
    local r = rgb[0]
    local g = rgb[1]
    local b = rgb[2];

    local sortrgb = clone rgb
    sortrgb.sort()
    local cmax = sortrgb[2].tofloat();
    local cmin = sortrgb[0];
    local delta = cmax - cmin;

    if(delta == 0){
        return [0, 0, cmax];
    }

    local hue;
    switch(cmax){
    case r:
        hue = ((g-b)/delta)%6;
        break;
    case g:
        hue = ((b-r)/delta) + 2;
        break;
    case b:
        hue = ((r-g)/delta) + 4;
        break;
    default:
        throw "Unexpected error, cmax not equal to r, g or b";
    }
    hue = hue * 60;

    return [hue, delta / cmax, cmax];
}

function hsvToRgb(hsv){
    local h = hsv[0];
    local s = hsv[1];
    local v = hsv[2];

    local c = s * v;
    local x = c * (1 - math.fabs((h / 60.0) % 2 - 1));
    local m = v - c;

    local r = m;
    local g = m;
    local b = m;

    server.log(format("C:%0.2f X:%0.2f M:%0.2f", c, x, m))

    if(h < 60){
        r += c;
        g += x;
    } else if(h < 120){
        r += x;
        g += c;
    } else if(h < 180){
        g += c;
        b += x;
    } else if(h < 240){
        g += x;
        b += c;
    } else if(h < 300){
        r += x;
        b += c;
    } else {
        r += c;
        b += x;
    }

    return [r, g, b];
}

function setHsv(hue, sat, val){
    local currHsv = rgbToHsv(currentLevels);
    local newHsv = [hue, sat, val];

    foreach(idx,v in currHsv){
        if(newHsv[idx] == null){
            newHsv[idx] = v;
        }
    }

    setLevels(hsvToRgb(newHsv));
}

function reportStatus(){
    lastReport = hardware.millis();
    agent.send("status", {
        "levels": currentLevels
    });
}

function setLevels(levels){
    local on = false;

    foreach(idx,level in levels){
        if(level < 0 || level > 1){
            server.log(format("Attempted to set invalid level %s: %.2f", ["R", "G", "B"][idx], level));
            continue;
        }

        leds[idx].write(math.pow(level, 2));
        currentLevels[idx] = level;
        on = on || level > 0
    }

    if(on){
        lastOnLevels = clone currentLevels;
    }
}

function isOn(){
    local on = false;
    foreach(idx,val in currentLevels){
        on = on || val > 0;
    }
    return on;
}

function power(){
    if(isOn()){
        setLevels([0.0, 0.0, 0.0]);
    } else if (lastOnLevels == null) {
        setLevels([DEFAULT_ON_LEVEL, DEFAULT_ON_LEVEL, DEFAULT_ON_LEVEL]);
    } else {
        setLevels(lastOnLevels);
    }
}

function saveDiy(idx, values){
    if(!("diy" in permValues)){
        permValues.diy <- {};
    }
    local diyData = permValues.diy;

    diyData[idx.tostring()] <- clone values;
    server.save(permValues);
}

function loadDiy(idx){
    if(!("diy" in permValues)){
        permValues.diy <- {};
    }
    local diyData = permValues.diy;

    local levels;
    if(!(idx.tostring() in diyData)){
        return [DEFAULT_ON_LEVEL, DEFAULT_ON_LEVEL, DEFAULT_ON_LEVEL];
    } else {
        return diyData[idx.tostring()];
    }
}

function doDim(){
    local current;
    local newLevels = [];
    local currTime = hardware.millis();
    local maxDiff = 0.0;
    local remaining = dimEndTime - currTime;

    if(remaining <= 0){
        server.log("DEBUG: Dim is done");
        setLevels(targetLevels);
        reportStatus();
        return;
    }

    local completed = 1.0 - (remaining.tofloat() / (dimEndTime - dimStartTime).tofloat());
    if (completed < 0){
        server.log("DEBUG: Completed is <0! " + completed);
        reportStatus();
        return;
    }

    foreach(idx,target in targetLevels){
        local start = startLevels[idx];

        // Find the maximum difference, so we can stop if we're basically there already
        local curr = currentLevels[idx];
        local diff = math.fabs(curr - target);

        maxDiff = maxDiff > diff ? maxDiff : diff

        newLevels.push(start + ((target - start) * completed));
    }

    setLevels(newLevels);

    // If we're basically set, just return
    if (maxDiff < 0.005){
        server.log(format("DEBUG: Dim is close enough start: %d end: %d now: %d",
            dimStartTime, dimEndTime, currTime));
        setLevels(targetLevels);
        reportStatus();
        return;
    }

    timer = imp.wakeup(((dimEndTime - dimStartTime) / DIM_STEPS).tofloat() / 1000, doDim);
    // Avoid reporting all the time -- kills timer consistency
    if(lastReport == null || math.abs(currTime - lastReport) > REPORT_INTERVAL){
        reportStatus();
    }
}

function startDim(time, levels){
    dimStartTime = hardware.millis();
    dimEndTime = dimStartTime + (time * 1000).tointeger();

    targetLevels = clone levels;
    startLevels = clone currentLevels;

    doDim();
}

function cancelDim(){
    if(timer != null){
        imp.cancelwakeup(timer);
    }
}


foreach(_,led in leds){
    led.configure(PWM_OUT, 1.0/FREQUENCY, 0.0);
}

function codeError(code, len, msg){
    local result = "";
	for (local i = 0; i < len; i++) {
		result += format("%d",code[i]);
	}

	server.log(format("Received %d-bit code with %s: %s", len, msg, result))
}

function executeCode(code){
    cancelDim();

    local spec = irCodes[code];
    switch(spec.type){
        case "color":
            server.log(format("Setting hue %.2f sat %.2f", spec.hue, spec.sat));
            setHsv(spec.hue, spec.sat, isOn() ? null : DEFAULT_ON_LEVEL);
            currDiy = null;
            break;
        case "valueChange":
            local hsv = rgbToHsv(currentLevels);
            setHsv(null, null, hsv[2] + spec.incr);
            break;
        case "levelChange":
            local newLevels = clone currentLevels;
            foreach(idx,delta in spec.levels){
                newLevels[idx] += delta;
            }
            setLevels(newLevels);
            break;
        case "power":
            power();
            break;
        case "diy":
            local idx = spec.idx;
            if(currDiy != null && currDiy == idx){
                local levels = clone currentLevels;
                saveDiy(spec.idx, levels);
                setLevels([0, 0, 0]);
                imp.wakeup(DIY_SET_PULSE, @() setLevels(levels));
            } else {
                currDiy = idx;
                setLevels(loadDiy(spec.idx));
            }
            break;
        default:
            server.log("Unexpected command type: " + spec.type);
    }
    reportStatus();
}

function onCode(code, len){
    local i

    if(len != 32){
        return codeError(code, len, "unexpected length");
    }

    local command = 0;
    for(i = 0; i < 8; i++){
        if(code[i] != 0){
            return codeError(code, len, "unexpected 1 in first byte");
        }
        if(code[i+8] == 0){
            return codeError(code, len, "unexpected 0 in second byte");
        }
        if((code[i+16] - code[i+24]) == 0){
            return codeError(code, len, "message not inverted correctly");
        }
        if(code[i+16] == 1) {
            command = command | (1 << (7-i))
        }
    }

    local key = format("%02X", command);
    if(!(key in irCodes)){
        server.log("Unknown command " + key);
        return;
    }

    executeCode(key);
}

function startup()
{
    server.log("Device starting");
    if ("getsoftwareversion" in imp)
    {
        server.log("Device firmware version: " + imp.getsoftwareversion());
    }
    else
    {
        server.log("Sorry - my firmware doesn't include imp.getsoftwareversion() yet.");
    }

    IR_receiver(irRx, 1, onCode);

    agent.on("set", function(data){
        cancelDim();

        if(data.time <= 0){
            setLevels(data.levels);
            reportStatus();
        } else {
            startDim(data.time, data.levels);
        }
    });

    agent.on("code", function(code){
        executeCode(code);
    })

}

startup();
