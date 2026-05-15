import uuid
import pytest
from helpers.api_client import ApiClient

pytestmark = pytest.mark.associations


def _payload(**overrides) -> dict:
    """Gera payload com nome único para isolar dados entre testes."""
    uid = uuid.uuid4().hex[:8].upper()
    base = {"name": f"Produto Assoc E2E {uid}", "status": "draft"}
    return {**base, **overrides}


class TestAtualizarCategorias:
    def test_dado_produto_criado__quando_atualizar_categorias_com_lista_vazia__entao_categoryIds_retorna_vazio(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-A01"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}/categories", json={"categoryIds": []})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["categoryIds"] == []

    def test_dado_payload_nulo_para_categorias__quando_enviar__entao_retorna_400(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-A03"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}/categories", data="null")

        # Assert
        assert resp.status_code == 400


class TestAtualizarTags:
    def test_dado_produto_criado__quando_atualizar_tags_com_lista_vazia__entao_tagIds_retorna_vazio(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-A02"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}/tags", json={"tagIds": []})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["tagIds"] == []

    def test_dado_payload_nulo_para_tags__quando_enviar__entao_retorna_400(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-A04"""
        # Arrange
        produto = product_factory(_payload())
        pid = produto["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}/tags", data="null")

        # Assert
        assert resp.status_code == 400
