#server = true
#bootstrap = true
#client_addr = "0.0.0.0"

service  {
  # Name for CBS in consul, env var CONFIG_BINDING_SERVICE
  # should be passed to Policy Agent app with this value
  Name = "config-binding-service"
  # Host name where CBS is running
  Address = "config-binding-service"
  # Port number where CBS is running
  Port = 10000
}