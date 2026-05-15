import pytest
from helpers.api_client import ApiClient

pytestmark = pytest.mark.crud

_PAYLOAD_BASE = {
    "name": "Produto E2E CRUD",
    "description": "Descricao de teste",
    "price": 49.90,
    "status": "draft",
    "stockQuantity": 10,
}


class TestCriarProduto:
    def test_dado_payload_valido__quando_criar_produto__entao_retorna_id_positivo(
        self, product_factory
    ):
        """E2E-C01a"""
        produto = product_factory(_PAYLOAD_BASE)

        assert produto["id"] > 0

    def test_dado_payload_valido__quando_criar_produto__entao_campos_correspondem_ao_payload(
        self, product_factory
    ):
        """E2E-C01b"""
        produto = product_factory(_PAYLOAD_BASE)

        assert produto["name"] == _PAYLOAD_BASE["name"]
        assert produto["price"] == _PAYLOAD_BASE["price"]
        assert produto["status"] == "draft"
        assert produto["stockQuantity"] == _PAYLOAD_BASE["stockQuantity"]


class TestBuscarProduto:
    def test_dado_produto_criado__quando_buscar_por_id__entao_retorna_produto_correto(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C02"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]

        # Act
        resp = api.get(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == pid
        assert data["name"] == _PAYLOAD_BASE["name"]
        assert data["description"] == _PAYLOAD_BASE["description"]

    def test_dado_produto_criado__quando_listar_produtos__entao_produto_aparece_na_lista(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C03"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]

        # Act
        resp = api.get(products_url)

        # Assert
        assert resp.status_code == 200
        ids = [p["id"] for p in resp.json()["items"]]
        assert pid in ids


class TestAtualizarProduto:
    def test_dado_produto_criado__quando_atualizar_nome__entao_nome_reflete_nova_valor(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C04a"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}", json={**_PAYLOAD_BASE, "name": "Nome Atualizado"})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["name"] == "Nome Atualizado"

    def test_dado_produto_criado__quando_atualizar_preco__entao_preco_reflete_novo_valor(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C04b"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}", json={**_PAYLOAD_BASE, "price": 99.99})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["price"] == 99.99


class TestDeletarProduto:
    def test_dado_produto_criado__quando_deletar__entao_retorna_204(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C05"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]

        # Act
        resp = api.delete(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code == 204

    def test_dado_produto_deletado__quando_buscar_por_id__entao_retorna_erro(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C06 — produto deletado pelo proprio teste; factory nao tenta deletar novamente"""
        # Arrange
        criado = product_factory(_PAYLOAD_BASE)
        pid = criado["id"]
        api.delete(f"{products_url}/{pid}")

        # Act
        resp = api.get(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code >= 400
