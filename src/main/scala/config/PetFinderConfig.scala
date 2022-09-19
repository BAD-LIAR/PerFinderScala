package config

final case class ServerConfig(host: String, port: Int)
final case class PetFinderConfig(db: DatabaseConfig, serverConfig: ServerConfig)
