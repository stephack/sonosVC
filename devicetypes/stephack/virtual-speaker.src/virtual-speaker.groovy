/**
 *  Virtual Speaker and Virtual Playlist Device Handler
 *
 *  Copyright 2017 Stephan Hackett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	
 */

metadata {
	definition (name: "Virtual Speaker", namespace: "stephack", author: "Stephan Hackett") {
	capability "Switch"
	capability "Refresh"
	capability "Switch Level"
	capability "Sensor"
	capability "Actuator"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
    			attributeState "off", label: '${name}', action: "switch.on", icon: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sp.png", backgroundColor: "#ffffff", nextState: "on"
		      	attributeState "on", label: '${name}', action: "switch.off", icon: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sp.png", backgroundColor: "#79b821", nextState: "off"
				//attributeState "turningOff", label: '${name}', action: "switch.on", icon: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sp.png", backgroundColor: "#ffffff", nextState: "turningOn"
		      	//attributeState "turningOn", label: '${name}', action: "switch.off", icon: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sp.png", backgroundColor: "#79b821", nextState: "turningOff"
        	}
            tileAttribute ("level", key: "SLIDER_CONTROL") {
			attributeState "level", action:"setLevel"
		}
		}
    	standardTile("refresh", "device.switch", decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
		details(["switch","refresh"])

	}

}

def on() {
	def isVPL = getDataValue("vpl")
	if(isVPL){
    	log.info "VPL pushed"
		parent.setSonosPL(isVPL)	
		sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
		sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false) //sets back to off (essentially making it a momentary device)
		//sendEvent(name: "momentary", value: "pushed", isStateChange: true)
    }
    else {
    log.info "VSP On"
    parent.setSonos("on")
	sendEvent(name: "switch", value: "on") //sets VSP or VPL to on
    }   
}

def off() {
	log.info "VSP Off"
	parent.setSonos("off")
	sendEvent(name: "switch", value: "off")    
}

def setLevel(val){
    log.info "VSP setLevel $val"
    on()
    parent.setSonosVol(val)
    sendEvent(name:"level",value:val)
}

def refresh() {
	parent.refresh(device.deviceNetworkId)
    log.info "refresh"
}