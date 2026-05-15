import os
import pytest
from helpers.api_client import ApiClient

_BASE_URL = os.getenv("LIFERAY_URL", "http://localhost:8080")
_AUTH = (
    os.getenv("LIFERAY_USER", "test@liferay.com"),
    os.getenv("LIFERAY_PASSWORD", "test"),
)
_PRODUCTS_PATH = "/o/product-rest/v1.0/sites/{site_id}/products"


@pytest.fixture(scope="session")
def api() -> ApiClient:
    return ApiClient(_BASE_URL, _AUTH)


@pytest.fixture(scope="session")
def site_id(api: ApiClient) -> int:
    # Prefer explicit env var to avoid discovery entirely
    if os.getenv("LIFERAY_SITE_ID"):
        return int(os.environ["LIFERAY_SITE_ID"])

    # Try headless-delivery (available when the module is deployed)
    resp = api.get("/o/headless-delivery/v1.0/sites/by-friendly-url-path/guest")
    if resp.status_code == 200:
        return resp.json()["id"]

    # Fallback: probe known default siteIds used by Liferay 7.4
    for candidate in (20117, 20122, 20099):
        probe = api.get(f"/o/product-rest/v1.0/sites/{candidate}/products")
        if probe.status_code == 200:
            return candidate

    raise RuntimeError(
        "Could not resolve siteId automatically. "
        "Set the LIFERAY_SITE_ID environment variable and retry."
    )


@pytest.fixture(scope="session")
def products_url(site_id: int) -> str:
    return _PRODUCTS_PATH.format(site_id=site_id)


@pytest.fixture
def product_factory(api: ApiClient, products_url: str):
    """Cria produto(s) e garante cleanup após o teste."""
    created_ids = []

    def _create(payload: dict) -> dict:
        resp = api.post(products_url, json=payload)
        resp.raise_for_status()
        data = resp.json()
        created_ids.append(data["id"])
        return data

    yield _create

    for pid in created_ids:
        api.delete(f"{products_url}/{pid}")
