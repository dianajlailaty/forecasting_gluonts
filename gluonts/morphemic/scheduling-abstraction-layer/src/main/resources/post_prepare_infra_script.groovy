def providedPortName = variables.get("providedPortName")
def providedPortValue = variables.get("providedPortValue")

if (providedPortName?.trim()){
    def ipAddr = new File(providedPortName+"_ip").text.trim()
    def publicProvidedPort = ipAddr + ":" + providedPortValue
    variables.put(providedPortName, publicProvidedPort)
    println("Provided variable " + providedPortName + "=" + publicProvidedPort)
}