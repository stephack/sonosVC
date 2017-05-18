/**
 *  Sonos Voice Command Child Creator - DO NOT PUBLISH !!
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
 * Speacial Thanks to @GuyInATie for allowing me to use snippets from his Sonos Remote Control smartApp. They were used
 * as the foundation of the code for retrieving and storing the recently played sation from the Sonos speakers.
 */

//			DO NOT PUBLISH !!!!!


definition(
    name: "Sonos Voice Command Child Creator",	//DO NOT PUBLISH !!!
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Child App Generator for Voice Commander - DO NOT PUBLISH!!",
    category: "My Apps",
    parent: "stephack:Sonos Voice Commander",
    iconUrl: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/child.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/child.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/child.png"
)

preferences {
	page(name: "chooseSpeaker", nextPage: whatsNext())
	page(name: "choosePlaylists", title: "Select stations for your Virtual Playlists", nextPage: confirmOptions)
	page(name: "confirmOptions", title: "Confirm All Settings Below")
}

def chooseSpeaker(){
	dynamicPage(name:"chooseSpeaker",uninstall:true){
    	section("Speaker to control with Virtual Devices") {
        	input "sonos", "capability.musicPlayer", title: "Please Choose Speaker", multiple:false, required: true, image: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sp.png"            
        }
        section("Create Virtual Speaker") {
        	input "vSpeaker", "text", title: "Create Virtual Speaker", description: "Enter VSP name here", multiple: false, required: true, image: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/vs.png"
            input "makeVPL", "bool", title: "Create Virtual Playlists?", submitOnChange: true
        }
        
        section("Set Custom App Name") {
			label title: "Assign a Custom App Name", required: false
      	}                  
	}
}

def choosePlaylists(){	
	dynamicPage(name: "choosePlaylists") {        
    	section{
            input "commTotal", "number", title: "# of VPL's to create", description:"Enter number: (1-5)", multiple: false, submitOnChange: true, required: true, image: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/pl.png", range: "1..5"
        }        
        if(commTotal && commTotal>=1 && commTotal<=5){
        	for(i in 1..commTotal) {
         		section("Virtual Playlist ${i}"){
        			input "vPlaylist${i}", "text", title: "Echo/Google Voice Name", description: "Enter Voice Command Here", multiple: false, required: true
            		input "tracks${i}","enum",title:"Sonos Playlist/Station to Run", description: "Tap to choose", required:true, multiple: false, options: stationSelections()
       			}
      		}
   		}
        else if(commTotal){
        	section{paragraph "Please choose a value between 1 and 5."}
        }
	}    
}

def confirmOptions(){
	dynamicPage(name:"confirmOptions",install:true, uninstall:true){    
		section("Speaker being Controlled"){
        		paragraph "${sonos}"
		}
		if(vSpeaker){
    		section("Virtual Speaker - Voice Commands"){
        		paragraph "'Alexa, turn on/off ${vSpeaker}'\n"+
        		"'Alexa, set the ${vSpeaker} to 25%'\n\n"+
                "If these commands don't roll off the tough, consider changing the name of your Virtual Speaker.\n\n"+
                "For any Virtual Playlists listed below, consider the same but focus on the 'ON' command. Use the Virtual Speaker name to control volume and turn off the speaker.\n"
        	}
		}		    
		if (makeVPL){
        	for(i in 1..commTotal) {
            	def currVPL = app."vPlaylist${i}"
                def currTrack = app."tracks${i}"
       			section("VPL ${i} - Alexa Commands"){
        			paragraph "'Alexa, turn on ${currVPL}'\n\n"+
                	"This will turn on ${sonos} and start the playlist/station [${currTrack}]"
       			}
           	}
    	}
        if(state.oldCommTotal>commTotal){
        	section("The following will be deleted"){
            	for(i in commTotal+1..state.oldCommTotal) {
            		def currVPL = app."vPlaylist${i}"
                	def currTrack = app."tracks${i}"
        			paragraph "${currVPL} with [${currTrack}] will be removed."                	
       			}
          	}       	
        }
        section("Advanced Options", hideable: true, hidden: true) {
        	input "listInThings", "bool", title: "Hide Virtual Devices in Things View?",description: "Hidden by default", defaultValue: true
            //paragragh "Only change this for advanced troubleshooting"
        }
	}
}

def whatsNext() {
	def nexPage = "confirmOptions"
	if (makeVPL){nexPage = "choosePlaylists"}
	return nexPage
}

def shallHide() { //unhide if feature configured
(settings["commTotal"]) ? false : true
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    if(state.oldCommTotal>commTotal){	//if less VPL's requested, then delete unneeded starting from the last backward
    	for (i in commTotal+1..state.oldCommTotal){        	
    		deleteVPL("${i}")
        }
    }
    if(listInThings!=state.OldListInThings){ //start rebuild if listInThings value has changed
    	deleteAllChildren()
    }
    initialize()
}

def initialize() {
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)	
	if (vSpeaker) {
       	createVSP()
	}    
    if(makeVPL && commTotal>0) {
    	createVPL()
        if(state.savedPList==null){state.savedPList=[]} 
    	savePlaylistsToState()
    	state.oldCommTotal = commTotal //keep track of number of voice commands to manage deleting if necessary
	}
    else{
    	deleteVPL("all")
    }
	subscribe(sonos, "status", setVsp)
 	subscribe(sonos, "level", setVspVol)
    state.OldListInThings = listInThings
    log.debug "Initialization Complete"
}

def defaultLabel() {
	return "${sonos} Commands"
}

def createVSP(){
	def childDevice = getAllChildDevices()?.find {
        it.device.deviceNetworkId == "VSP_${app.id}"
       	}        
        if (!childDevice) {
            childDevice = addChildDevice("stephack", "Virtual Speaker", "VSP_${app.id}", null,[completedSetup: true,
            label: vSpeaker, isComponent: listInThings, componentName: vSpeaker, componentLabel: vSpeaker]) 
            childDevice.refresh()            
            log.info "Creating VSP [${childDevice}]"            
		}
        else {
            childDevice.label = vSpeaker
            childDevice.name = vSpeaker
            log.info "VSP renamed to [${vSpeaker}]"
		}
}

def createVPL() {
	for(i in 1..commTotal) {
        def currVPL = app."vPlaylist${i}"
    	def childDevice = getAllChildDevices()?.find {
        it.device.deviceNetworkId == "VPL_${app.id}-${i}"
       	}        
        if (!childDevice) {
            childDevice = addChildDevice("stephack", "Virtual Speaker", "VPL_${app.id}-${i}", null,[completedSetup: true,
            label: currVPL, isComponent: listInThings, componentName: currVPL, componentLabel: currVPL, "data":["vpl":"${i}"]]) 
            childDevice.refresh()            
            log.info "Creating VPL${i} [${childDevice}]"            
		}
        else {
            childDevice.label = currVPL
            childDevice.name = currVPL
            log.info "VPL renamed to [${currVPL}]"
		}
	}
}

def deleteVPL(which){	//removes unneeded VPL starting with the highest number going backward (*previoulsy chosen values areleft intact for easy readding if necessary...may need to wipe these settings TBD)
	log.info "start delete"
	if(which=="all"){    	
    	def childDevice = getAllChildDevices()?.findAll{
        	it.deviceNetworkId.startsWith("VPL")
       	}
        if (childDevice) {
        	log.info "Deleting all Virtual Playlists: ${childDevice}"
        	childDevice.each {child->
  				deleteChildDevice(child.deviceNetworkId)            
    		}
        }
    }
    else{
    	def childDevice = getAllChildDevices()?.find{
        	it.deviceNetworkId.startsWith("VPL") && it.deviceNetworkId.endsWith("${which}") 
       	}
        log.info "Deleting VPL: [${childDevice}]"
        childDevice?deleteChildDevice(childDevice.deviceNetworkId):""
    }
    
  	//for(i in 1..childDevice.size()){
    //	app.updateSetting("vPlaylist${i}", "")
     //  	app.updateSetting("tracks${i}", "")
   // }
   // app.updateSetting("commTotal", "")
        
}

def deleteAllChildren(){ //deletes all children when switches fron isComponent true to false or vice versa
	def childDevice = getAllChildDevices()       	
        if (childDevice) {
        	log.info "Deleting all child Devices"
        	childDevice.each {child->
  				deleteChildDevice(child.deviceNetworkId)            
    		}
        }
}

def refresh(dni) {
	log.info "Getting status update to refresh VSP"
    def sonosStat = ""
    sonos.currentStatus=="playing"?sonosStat=="on":sonosStat=="off"    
    def childDevice = getAllChildDevices()?.find {
        it.device.deviceNetworkId == dni
       	}
    childDevice.sendEvent(name:"switch",value:sonosStat)
    childDevice.sendEvent(name:"level",value:sonos.currentLevel)
}

def setSonos(evt) {	//sends VSP states to Sonos speaker
    	evt=="on"?sonos.play():sonos.pause()
        log.info "VSP sent [$evt.value] status update to Speaker"
}

def setSonosVol(evt) {	//sends VSP volume changes to Sonos speaker
	sonos.unmute()
    sonos.setLevel(evt)
   	log.info "VSP sent volume [$evt.value] update to Speaker"
}

def setSonosPL(evt) {	//sends VPL station requests to Sonos speaker
    def currTrack = app."tracks${evt}"
    selectPlaylist(currTrack)
    log.info "VPL $evt.value requested [$currTrack] playlist on Sonos Speaker"
}

def setVsp(evt) {	//syncs VSPs to Sonos states
	def childDevice = getAllChildDevices()?.find {
        it.device.deviceNetworkId == "VSP_${app.id}"
   	}
    evt.value=="playing"?childDevice.sendEvent(name:"switch",value:"on"):childDevice.sendEvent(name:"switch",value:"off")
    log.info "Speaker sent [$evt.value] status update to VSP"    
}

def setVspVol(evt) { //syncs VSP to Sonos volume changes
	def childDevice = getAllChildDevices()?.find {
        it.device.deviceNetworkId == "VSP_${app.id}"
   	}
    childDevice.sendEvent(name:"level",value: evt.value)
    log.info "Speaker sent volume [$evt.value] update to VSP"
}

def stationSelections(){	//retrieves recently played stations from sonos speaker and presents as options when choosing station in VPLs
    def newOptions = state.savedPList.findAll()//{it.selected==true}	//ensures previously selected stations are included in the new list
    def states = sonos.statesSince("trackData", new Date(0), [max:30]) //??gets all previously played tracks from sonos speaker
    states?.each{	//for each track in the fresh list
    	def stationData = it.jsonValue
        if(!newOptions.find{fav->fav.station==stationData.station}){ //checks whether previously selected tracks(newOptions) exist in the new list and prevents adding second entry
   			newOptions << [uri:stationData.uri,metaData:stationData.metaData,station:stationData.station]
        }
    }
    def options = newOptions.collect{it.station?:"N/A"}.sort()
    state.savedPList = newOptions
	return options
}

def savePlaylistsToState(){	//stores all the stations selected by the VPLs in savedPList state
	log.info "Saving Playlist info"
    def newStationList = []
    for(i in 1..commTotal){
        def placeHold = app."tracks${i}"
    	if(placeHold){
        	newStationList << state.savedPList.find{it.station==placeHold} // add each selected station to savedPList state
        }
   	}
    state.savedPList = newStationList
}

def selectPlaylist(pList){	//receives playlist request from VPL and finds detailed track info stored in savedPList state
  	def myStations = state.savedPList
    if(myStations.size==0){
		log.debug "No Saved Playlists/Stations Found"
    }
    else{ 
    	def stationToPlay = myStations.find{stationToPlay->stationToPlay.station==pList}
    	playStation(stationToPlay)
    }    
}

def isPlaylistOrAlbum(trackData){ //determines type of playlist/station and formats properly for playback
	trackData.uri.startsWith('x-rincon-cpcontainer') ? true : false
}

def playStation(trackData){	//sends formatted play command to Sonos speaker
    if(isPlaylistOrAlbum(trackData)){
		log.debug "Working around some sonos device handler playlist wierdness ${trackData.station}. This seems to work"
        trackData.transportUri=trackData.uri
        trackData.enqueuedUri="savedqueues"
        trackData.trackNumber="1"
        sonos.setTrack(trackData)
        pause(1500)
        sonos.play()
    }
    else{
    	sonos.playTrack(trackData.uri,trackData.metaData)
    }
}

def toJson(str){
    def slurper = new groovy.json.JsonSlurper()
    def json = slurper.parseText(str)
}


/*
def getDescription(dNumber) {	
    def descript = "Tap to Configure"
    def currVPL = app."vPlaylist${dNumber}"
    def currTrack = app."tracks${dNumber}"    
	if(currVPL) {
    	def abbrVPL = currVPL.length()<=25?"${currVPL}":"${currVPL}".substring(0, 25)+".."
        def abbrTrack = currTrack.length()<=25?"${currTrack}":"${currTrack}".substring(0, 25)+".."
    	descript = "NAME:  [${abbrVPL}]\nPLAYS: ${abbrTrack}\nTap to edit"
   	}
	return descript
}
*/
