service {
  # Name for CBS in consul, env var CONFIG_BINDING_SERVICE
  # should be passed to dfc app with this value
  # This is only to be used when contacting cbs via local host
  # (typicall when dfc is executed as an application without a container)
  Name = "config-binding-service-localhost"
  # Host name where CBS is running
  Address = "localhost"
  # Port number where CBS is running
  Port = 10000
}