import uuid
import pytest
from helpers.api_client import ApiClient

pytestmark = pytest.mark.validation


def _payload(**overrides) -> dict:
    """Gera payload mínimo com nome único para isolar dados entre testes."""
    uid = uuid.uuid4().hex[:8].upper()
    return {"name": f"Produto Defaults E2E {uid}", **overrides}


class TestValoresDefault:
    def test_dado_payload_so_com_nome__quando_criar__entao_price_e_zero(
        self, product_factory
    ):
        """E2E-V01a"""
        # Arrange / Act
        produto = product_factory(_payload())

        # Assert
        assert produto["price"] == 0.0

    def test_dado_payload_so_com_nome__quando_criar__entao_stockQuantity_e_zero(
        self, product_factory
    ):
        """E2E-V01b"""
        # Arrange / Act
        produto = product_factory(_payload())

        # Assert
        assert produto["stockQuantity"] == 0

    def test_dado_payload_so_com_nome__quando_criar__entao_status_e_draft(
        self, product_factory
    ):
        """E2E-V01c"""
        # Arrange / Act
        produto = product_factory(_payload())

        # Assert
        assert produto["status"] == "draft"


class TestPayloadInvalido:
    def test_dado_body_nulo__quando_criar_produto__entao_retorna_400(
        self, api: ApiClient, products_url: str
    ):
        """E2E-V02"""
        # Act
        resp = api.post(products_url, data="null")

        # Assert
        assert resp.status_code == 400

    def test_dado_body_nulo__quando_atualizar_produto__entao_retorna_400(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-V03"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}", data="null")

        # Assert
        assert resp.status_code == 400


class TestSiteIdInvalido:
    def test_dado_siteId_invalido__quando_criar_produto__entao_retorna_erro(
        self, api: ApiClient
    ):
        """E2E-V04"""
        # Act
        resp = api.post(
            "/o/product-rest/v1.0/sites/999999999/products",
            json=_payload(),
        )

        # Assert
        assert resp.status_code >= 400

    def test_dado_productId_invalido__quando_buscar__entao_retorna_erro(
        self, api: ApiClient, products_url: str
    ):
        """E2E-V05"""
        # Act
        resp = api.get(f"{products_url}/999999999")

        # Assert
        assert resp.status_code >= 400


class TestProdutoDeOutroSite:
    def test_dado_produto_de_outro_site__quando_acessar_com_siteId_diferente__entao_retorna_400(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-V06"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.get(f"/o/product-rest/v1.0/sites/999999999/products/{pid}")

        # Assert
        assert resp.status_code >= 400
