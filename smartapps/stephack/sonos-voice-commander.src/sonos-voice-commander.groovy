/**
 *  Sonos Voice Commander
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



definition(
    name: "Sonos Voice Commander",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Configure Voice Commands for your Sonos speakers",
    category: "My Apps",
    //parent: "stephack:Sonos Voice Commander",
    iconUrl: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png"
)

preferences {
	page(name: "startPage")
	page(name: "parentPage")
	page(name: "mainPage", nextPage: whatsNext())
	page(name: "choosePlaylists", title: "Select stations for your Virtual Playlists", nextPage: confirmOptions)
	page(name: "confirmOptions", title: "Confirm All Settings Below")
    page(name: "aboutPage")
}

def startPage() {
    if (parent) {
        mainPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Installed Voice Commands") {
            app(name: "childApps", appName: appName(), namespace: "stephack", title: "Create New Voice Command", multiple: true)
        }
       // section("Version Info & User's Guide") {
       //		href (name: "aboutPage", 
       //		title: "Sonos Voice Commander\nVersion 0.1.20170504", 
       //		description: "Tap for User's Guide and Info.",
       //		image: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
       //		required: false,
       //		page: "aboutPage"
 	   //		)
      //	}
    }
}

private def appName() { return "${parent ? "VC Config" : "Sonos Voice Commander"}" }

def mainPage(){
	dynamicPage(name:"mainPage",uninstall:true){
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
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }  
}

def initChild() {
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

def initParent() {
	log.debug "Parent Initialized"
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

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none) {
     	section("User's Guide: Sonos Voice Link") {
        	paragraph "This smartApp allows you to create voice commands for your integrated Sonos speakers. These commands are available to connect"+
            " with other smartApps like Alexa and Google Home. There are 2 types of 'Voice Commands' you can create."
        }
        section("1. Virtual Speakers"){
        	paragraph "These allow you to turn a Sonos speaker On(Play) and Off(Pause) similar to the native Sonos integration.\n"+
            "However this device type is exposed as a dimmable switch and allows you to also set the speaker volume.\nSee Best Practices below."
        }
        section("2. Virtual Playlists"){
        	paragraph "These allow you to turn on the speaker and automatically start playing a station or playlist.\n"+
            "While these are also exposed as dimmable switches, they should be used more like station presets buttons. They do NOT process 'OFF' commands.\nSee Best Practices below."
		}
		section("Best Practices:"){
        	paragraph "1. You should set your Virtual Speaker name to the voice command you will use with your Echo/Google Home.\n\n"+
			" - 'Alexa, turn on/off [Dining Room Speaker]'\n"+
			" On=play; Off=pause\n"+
			" - 'Alexa, set [Dining Room Speaker] to 35'\n"+
			" Sets speaker volume to 35.\n\n"+
            "2. You should set your Virtual Playlist name to the voice command you would use to start playback of a particular station."+
            " While it can be used for volume control, it would be more practical to use the Virtual Speaker for that instead.\n"+
            "By design, it cannot be used to turn off the speaker. Again, the Virtual Speaker should be used instead.\n\n"+
            " - 'Alexa, turn on [Jazz in the Dining Room]'\n"+
            " Starts playback of the associated Jazz station."
 		}
	}
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