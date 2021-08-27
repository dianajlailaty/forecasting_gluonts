// Calculing privatekey fingerprint
println "== Baguette request preparation"
println "-- Getting parameters"
def password = credentials.containsKey('target_password') ? credentials.get('target_password') : ""
File privateKeyFile = new File("/tmp/ems-keypair")
File ipFile = new File("/tmp/ip.txt")
if (!privateKeyFile.exists()) {
    println "ERR: PrivateKey file doesn't exist. Exiting"
    return 255
}
if (!ipFile.exists()) {
    println "ERR: Public ip file doesn't exist. Exiting"
    return 255
}
def privateKey = privateKeyFile.text
def ip = ipFile.text

def fingerprint = '' // TODO:

println "-- Validating parameters"
// Chacking parameters values
def baguetteBearer = variables.get('authorization_bearer');
def baguetteIp = variables.get('baguette_ip');
def baguettePort = variables.get('baguette_port');
def usingHttps = variables.get("using_https").toBoolean()
def emsUrl = String.format("%s://%s:%s/baguette/registerNode", usingHttps ? "https" : "http",baguetteIp,baguettePort)
def os = variables.get("target_operating_system")
//def ip = variables.get("target_ip")
def port = 22 //variables.get("target_port")
def username = "whoami".execute().text[0..-2] // We capture the output of whoami command & remove the \n char
def type = variables.get("target_type")
def name = variables.get("target_name")
def provider = variables.get("target_provider")
def location = variables.get("location")
def id = variables.get("id")

// Request preparation
def isInputValid = true;
isInputValid &= (baguetteBearer != null);
isInputValid &= (baguetteIp != null);
isInputValid &= (baguettePort != null);
isInputValid &= (emsUrl != null);
isInputValid &= (os != null);
isInputValid &= (ip != null);
isInputValid &= (port != null);
isInputValid &= (username != null);
isInputValid &= (type != null);
isInputValid &= (name != null);
isInputValid &= (provider != null);

if (!isInputValid) {
    println "ERR: One or many provided parameters are invalid."
    return 255;
}

println "-- payload preparation"
def requestPayload = [:]
requestPayload.operatingSystem = os;
requestPayload.address = ip;
requestPayload.ssh = [:];
requestPayload.ssh.port = port;
requestPayload.ssh.username = username;
requestPayload.type = type;
requestPayload.name = name;
requestPayload.provider = provider;
requestPayload.timestamp = new Date().getTime();
requestPayload.location = location;
requestPayload.id = id;

//if (password != "") {
//    println "INFO: Using provided password"
requestPayload.ssh.password = password;
//}
requestPayload.ssh.key = privateKey;
requestPayload.ssh.fingerprint = fingerprint;

def requestContent =  groovy.json.JsonOutput.toJson(requestPayload);
println requestContent
// Request execution
println "== Requesting baguette server for EMS deployment"
def emsConnection = new URL(emsUrl).openConnection();
emsConnection.setRequestMethod("POST")
emsConnection.setDoOutput(true)
emsConnection.setRequestProperty("Content-Type", "application/json")
emsConnection.setRequestProperty("Authorization", "Bearer " + baguetteBearer)
emsConnection.getOutputStream().write(requestContent.getBytes("UTF-8"));
def responseCode = emsConnection.getResponseCode();
def responseContent = emsConnection.getInputStream().getText();

// Feedback analysis
println "== Obtaining result:"
println ">> Result: Code=" + responseCode + " Content=" + responseContent
result = '{"Code"="' + responseCode + '","Content"="' + responseContent + '"}'