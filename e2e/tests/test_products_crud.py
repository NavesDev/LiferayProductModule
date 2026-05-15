import uuid
import pytest
from helpers.api_client import ApiClient

pytestmark = pytest.mark.crud


def _payload(**overrides) -> dict:
    """Gera payload com nome único para isolar dados entre testes."""
    uid = uuid.uuid4().hex[:8].upper()
    base = {
        "name": f"Produto E2E CRUD {uid}",
        "description": "Descricao de teste",
        "price": 49.90,
        "status": "draft",
        "stockQuantity": 10,
    }
    return {**base, **overrides}


class TestCriarProduto:
    def test_dado_payload_valido__quando_criar_produto__entao_retorna_id_positivo(
        self, product_factory
    ):
        """E2E-C01a"""
        # Arrange
        payload = _payload()

        # Act
        produto = product_factory(payload)

        # Assert
        assert produto["id"] > 0

    def test_dado_payload_valido__quando_criar_produto__entao_campos_correspondem_ao_payload(
        self, product_factory
    ):
        """E2E-C01b"""
        # Arrange
        payload = _payload()

        # Act
        produto = product_factory(payload)

        # Assert
        assert produto["name"] == payload["name"]
        assert produto["price"] == payload["price"]
        assert produto["status"] == "draft"
        assert produto["stockQuantity"] == payload["stockQuantity"]


class TestBuscarProduto:
    def test_dado_produto_criado__quando_buscar_por_id__entao_retorna_produto_correto(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C02"""
        # Arrange
        payload = _payload()
        criado = product_factory(payload)
        pid = criado["id"]

        # Act
        resp = api.get(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == pid
        assert data["name"] == payload["name"]
        assert data["description"] == payload["description"]

    def test_dado_produto_criado__quando_listar_produtos__entao_produto_aparece_na_lista(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C03 — busca por nome único para não depender da paginação padrão"""
        # Arrange
        payload = _payload()
        criado = product_factory(payload)
        pid = criado["id"]

        # Act
        resp = api.get(products_url, params={"search": payload["name"]})

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
        payload = _payload()
        criado = product_factory(payload)
        pid = criado["id"]
        novo_nome = f"Nome Atualizado {uuid.uuid4().hex[:6].upper()}"

        # Act
        resp = api.put(f"{products_url}/{pid}", json={**payload, "name": novo_nome})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["name"] == novo_nome

    def test_dado_produto_criado__quando_atualizar_preco__entao_preco_reflete_novo_valor(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C04b"""
        # Arrange
        payload = _payload()
        criado = product_factory(payload)
        pid = criado["id"]

        # Act
        resp = api.put(f"{products_url}/{pid}", json={**payload, "price": 99.99})

        # Assert
        assert resp.status_code == 200
        assert resp.json()["price"] == 99.99


class TestDeletarProduto:
    def test_dado_produto_criado__quando_deletar__entao_retorna_204(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C05"""
        # Arrange
        criado = product_factory(_payload())
        pid = criado["id"]

        # Act
        resp = api.delete(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code == 204

    def test_dado_produto_deletado__quando_buscar_por_id__entao_retorna_erro(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-C06 — produto deletado pelo próprio teste; factory não tenta deletar novamente"""
        # Arrange
        criado = product_factory(_payload())
        pid = criado["id"]
        api.delete(f"{products_url}/{pid}")

        # Act
        resp = api.get(f"{products_url}/{pid}")

        # Assert
        assert resp.status_code >= 400
