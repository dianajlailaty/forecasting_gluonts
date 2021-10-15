PROVIDED_PORT_NAME=$variables_providedPortName

if [[ ! -z $PROVIDED_PORT_NAME ]]; then
    IP_ADDR=$(dig +short myip.opendns.com @resolver1.opendns.com)
    echo Public adress: $IP_ADDR

    echo "$IP_ADDR" > $PROVIDED_PORT_NAME"_ip"
fi