import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson
import static groovyx.net.http.ContentType.JSON

@Grab(group='org.codehaus.groovy.modules.http-builder',
        module='http-builder',
        version='0.7.1')

def processorName = 'PollFTP'
def host = '127.0.0.1'
def port = 8080
def nifi = new RESTClient("http://127.0.0.1:8080/nifi-api/")

// upload template

//curl -iv -F template=@Full.xml POST http://127.0.0.1:8080/nifi-api/process-groups/root/templates/upload

//println 'Uploading template...'
//def resp_root = nifi.get(
//	path: 'process-groups/root'
//)
//def id_root = resp_root.data.id
//println "Root process group id:  $id_root"

//def file = new File("Full.xml").getText("UTF-8")
//def resp_upload = nifi.post(
//	path: "process-groups/$id_root/templates/upload",
//	body: file,
//	requestContentType: URLENC
//)
//assert resp_upload.status == 200
//println "Template $resp_upload.data.name created with id: $resp_upload.data.id"
println 'Looking up for template'
resp_template = nifi.get(
	path: "flow/templates"
)
assert resp_template.status == 200
def templId = resp_template.data.templates[0].id
println 'Template Id: '  + templId

// instantiate template
println 'Instantiating the template'
def builder_Insta  = new JsonBuilder()
builder_Insta {
    templateId templId
    originX '2830'
    originY  '1050'
}
resp_Insta = nifi.post(
    path: "process-groups/root/template-instance",
    body: builder_Insta.toPrettyString(),
   requestContentType: JSON
)

println 'Template instantiated'



println 'Looking up the ControllerServices...'
def resp_controller = nifi.get(
    path: 'flow/process-groups/root/controller-services'
//    ,
//    query: [q: processorName]
)

// println prettyPrint(toJson(resp.data))

assert resp_controller.status == 200
//assert resp.data.searchResultsDTO.processorResults.size() == 1
// println prettyPrint(toJson(resp.data))

println(resp_controller.data.controllerServices.size() )
//println prettyPrint(toJson(resp.data))

def builder_Distributed = new JsonBuilder()

for (int i = 0; i < resp_controller.data.controllerServices.size(); i++){

//def processorId = resp.data.controller.processorResults[0].id
//def processGroup= resp.data.searchResultsDTO.processorResults[0].groupId
//println "Found the component, id/group:  $processorId/$processGroup"

println 'CONTROLLER'
//println(prettyPrint(toJson(resp_controller.data.controllerServices[i])))
println(prettyPrint(toJson(resp_controller.data.controllerServices[i].component.state)))
println(prettyPrint(toJson(resp_controller.data.controllerServices[i].component.state)))
controllerId = resp_controller.data.controllerServices[i].component.id
clientId = resp_controller.data.controllerServices[i].revision.clientId
parentGroupId = resp_controller.data.controllerServices[i].component.parentGroupId
version=resp_controller.data.controllerServices[i].revision.version

 println(resp_controller.data.controllerServices[i].component.id) + controllerId
 println 'Ref size: ' + (resp_controller.data.controllerServices[i].component.referencingComponents.size()) 
 

//if (resp_controller.data.controllerServices[i].component.state == "DISABLED"){
if (resp_controller.data.controllerServices[i].component.referencingComponents.size() != 0 ){
println 'Enabling the controller...'
builder_Distributed {
     revision {
                        clientId "$clientId"
                        version 0
              }
    id "$controllerId"        
    component {
	id "$controllerId"
       parentGroupId "$parentGroupId"   
        state "ENABLED"
    }
}

//println prettyPrint(toJson(resp_controller.controllerServices[i].data))
resp_Distributed = nifi.put(
    path: "controller-services/$controllerId",
    body: builder_Distributed.toPrettyString(),
    requestContentType: JSON
)
//assert resp_Distributed.status == 200
println 'Controller ENABLED'

}

if (resp_controller.data.controllerServices[i].component.referencingComponents.size() == 0){
		if (resp_controller.data.controllerServices[i].component.state == "ENABLED"){
			println 'Enabling the controller...'
			builder_Distributed {
			     revision {
                        	clientId "$clientId"
                        	version 0
	              }
		    id "$controllerId"        
	    component {
		id "$controllerId"
	       parentGroupId "$parentGroupId"   
        	state "DISABLED"
    		}
		 }
		 //println prettyPrint(toJson(resp_controller.controllerServices[i].data))
	resp_Distributed = nifi.put(
	    path: "controller-services/$controllerId",
	    body: builder_Distributed.toPrettyString(),
	    requestContentType: JSON
		)
//assert resp_Distributed.status == 200
println 'Controller DISABLED'
		}

//println prettyPrint(toJson(resp_controller.controllerServices[i].data))
resp_Distributed = nifi.put(
    path: "controller-services/$controllerId",
    body: builder_Distributed.toPrettyString(),
    requestContentType: JSON
)
//assert resp_Distributed.status == 200
println 'Controller ENABLED'

println 'Deleting the controller...'
builder_Distributed {
     revision {
                        clientId "$clientId"
                        version 0
              }
    id "$controllerId"        
    component {
	id "$controllerId"
       parentGroupId "$parentGroupId"   
    }
}

println (version)
println (clientId)
//println prettyPrint(toJson(resp_controller.controllerServices[i].data))
resp_Distributed = nifi.delete(
    path: "controller-services/$controllerId",
    query: ['version': version,
	    'clientId': clientId]
)
//assert resp_Distributed.status == 200
println 'Controller DELETED'
}

//println(prettyPrint(toJson(resp_controller.data.controllerServices[i].component.state)))
}

println 'Distributed ok'


println 'Looking up the processors...'
def resp = nifi.get(
    path: 'flow/search-results'
//    ,
//    query: [q: processorName]
)
assert resp.status == 200
//assert resp.data.searchResultsDTO.processorResults.size() == 1
// println prettyPrint(toJson(resp.data))

println(resp.data.searchResultsDTO.processorResults.size() )
//println prettyPrint(toJson(resp.data))

for (int i = 0; i < resp.data.searchResultsDTO.processorResults.size(); i++){
	j = toJson(resp.data.searchResultsDTO.processorResults[i])
//	println(prettyPrint(j))
	if (resp.data.searchResultsDTO.processorResults[i].matches[7] == "Type: GetFTP"){
		println("YES")
		println(resp.data.searchResultsDTO.processorResults[i].id)
		println(resp.data.searchResultsDTO.processorResults[i].name)
		def processorId = resp.data.searchResultsDTO.processorResults[i].id
                def processGroup= resp.data.searchResultsDTO.processorResults[i].groupId
                println "Found the component, id/group:  $processorId/$processGroup"
                
                println 'Preparing to update the flow state...'
                resp_state = nifi.get(path: "processors/$processorId")
                assert resp_state.status == 200
                
                println resp_state.data.revision.version
                println resp_state.data.component.state
                
                if (resp_state.data.component.state == "STOPPED"){
                	println 'Starting the processor...'
                	def builder = new JsonBuilder()
                builder {
                    revision {
                        clientId 'clientId'
                        version resp_state.data.revision.version
                    }
                    component {
                        id "$processorId"
                        state "RUNNING"
                        config {
                        	properties {
                      'Hostname' 'xyz'    
                			'Password' 'xyz'
                        	}
                        }
                    }
                }
                resp_processor = nifi.put(
                    path: "processors/$processorId",
                    body: builder.toPrettyString(),
                    requestContentType: JSON
                )
                assert resp_processor.status == 200
                }
                
                println 'Ok'
	}
	
	
	println("YES")
		println(resp.data.searchResultsDTO.processorResults[i].id)
		println(resp.data.searchResultsDTO.processorResults[i].name)
		def processorId = resp.data.searchResultsDTO.processorResults[i].id
                def processGroup= resp.data.searchResultsDTO.processorResults[i].groupId
                println "Found the component, id/group:  $processorId/$processGroup"
                
                println 'Preparing to update the flow state...'
                resp_state = nifi.get(path: "processors/$processorId")
                assert resp_state.status == 200
                
                println resp_state.data.revision.version
                println resp_state.data.component.state
                
                if (resp_state.data.component.state == "STOPPED"){
                	println 'Starting the processor...'
                	def builder = new JsonBuilder()
                builder {
                    revision {
                        clientId 'clientId'
                        version resp_state.data.revision.version
                    }
                    component {
                        id "$processorId"
                        state "RUNNING"
                        }
                    }
                
                
                resp_processor = nifi.put(
                    path: "processors/$processorId",
                    body: builder.toPrettyString(),
                    requestContentType: JSON
                )
                assert resp_processor.status == 200
                
              
                println 'Ok'
	}
	
	
	
}
