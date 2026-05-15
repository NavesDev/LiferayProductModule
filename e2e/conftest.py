import os
import pytest
from helpers.api_client import ApiClient

_TAXONOMY_VOCAB_PATH = "/o/headless-admin-taxonomy/v1.0/sites/{site_id}/taxonomy-vocabularies"
_TAXONOMY_CAT_PATH = "/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/{vocab_id}/taxonomy-categories"

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


@pytest.fixture(scope="session")
def category_id(api: ApiClient, site_id: int) -> int:
    """Returns an existing category ID, or creates one if none exist."""
    vocab_resp = api.get(_TAXONOMY_VOCAB_PATH.format(site_id=site_id))
    if vocab_resp.status_code == 200:
        for vocab in vocab_resp.json().get("items", []):
            cat_resp = api.get(_TAXONOMY_CAT_PATH.format(vocab_id=vocab["id"]))
            if cat_resp.status_code == 200:
                items = cat_resp.json().get("items", [])
                if items:
                    return items[0]["id"]

    vocab_resp = api.post(
        _TAXONOMY_VOCAB_PATH.format(site_id=site_id),
        json={"name": "E2E Test Vocab"},
    )
    vocab_resp.raise_for_status()
    cat_resp = api.post(
        _TAXONOMY_CAT_PATH.format(vocab_id=vocab_resp.json()["id"]),
        json={"name": "E2E Test Category"},
    )
    cat_resp.raise_for_status()
    return cat_resp.json()["id"]


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
