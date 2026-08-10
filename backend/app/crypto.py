from cryptography.fernet import Fernet, InvalidToken

from app.config import settings

_fernet = Fernet(settings.token_encryption_key.encode())


def encrypt_token(raw_token: str) -> str:
    return _fernet.encrypt(raw_token.encode()).decode()


def decrypt_token(encrypted_token: str) -> str:
    try:
        return _fernet.decrypt(encrypted_token.encode()).decode()
    except InvalidToken as exc:
        raise ValueError("Stored WiGLE token could not be decrypted.") from exc
