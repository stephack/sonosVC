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
 */
definition(
    name: "Sonos Voice Commander",
    singleInstance: true,
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Configure Voice Commands for your Sonos speakers",
    category: "My Apps",
    iconUrl: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png")
	
    

preferences {
	page(name: "mainPage", title: "Installed Sonos Voice Commands")
   	page(name: "aboutPage")
}

def mainPage(){
    dynamicPage(name: "mainPage", install: true, uninstall: true, submitOnChange: true) {
        section {
            app(name: "ChildApp", appName: "Sonos Voice Command Child Creator", namespace: "stephack", title: "Create New Voice Command", multiple: true)
        }
        section("Version Info & User's Guide") {
       		href (name: "aboutPage", 
       		title: "Sonos Voice Commander\nVersion 0.1.20170504", 
       		description: "Tap for User's Guide and Info.",
       		image: "https://cdn.rawgit.com/stephack/sonosVC/master/resources/images/sonosVC.png",
       		required: false,
       		page: "aboutPage"
 	   		)
    	}
  
  	}
}

def installed(){
	initialize()
}

def updated(){
	initialize()
}

def initialize(){
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