import httpx

# Confirmed against WiGLE's published API schema (api.wigle.net/swagger.json).
# The bare https://wigle.net domain is the website, not the API - it does not
# accept Basic Auth or multipart uploads.
WIGLE_API_BASE = "https://api.wigle.net/api/v2"


class WigleAuthError(Exception):
    """Raised when WiGLE rejects credentials or a file upload."""


async def verify_credentials(api_name: str, api_token: str) -> dict:
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(
            f"{WIGLE_API_BASE}/profile/user",
            auth=(api_name, api_token),
            headers={"Accept": "application/json"},
        )

    if response.status_code == 200:
        return response.json()
    if response.status_code == 401:
        raise WigleAuthError("Invalid WiGLE API name or token.")
    raise WigleAuthError(f"Unexpected WiGLE response: HTTP {response.status_code}")


async def upload_session_file(api_name: str, api_token: str, file_bytes: bytes, file_name: str) -> dict:
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(
            f"{WIGLE_API_BASE}/file/upload",
            auth=(api_name, api_token),
            files={"file": (file_name, file_bytes, "text/csv")},
        )

    if response.status_code != 200:
        raise WigleAuthError(f"WiGLE upload rejected (HTTP {response.status_code}): {response.text}")

    body = response.json()
    if not body.get("success", False):
        raise WigleAuthError(f"WiGLE upload reported failure: {body}")
    return body
