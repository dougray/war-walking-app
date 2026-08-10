from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str = "postgres://researcher_admin:change-me@warwalking-db:5432/warwalking_fitness"
    token_encryption_key: str

    class Config:
        env_file = ".env"


settings = Settings()
