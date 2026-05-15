import uuid
import pytest
from helpers.api_client import ApiClient

pytestmark = pytest.mark.filters


def _uid() -> str:
    """Gera sufixo unico para isolar dados entre runs paralelas."""
    return uuid.uuid4().hex[:8].upper()


def _items(resp) -> list:
    """Extrai a lista de itens da resposta paginada da API."""
    return resp.json().get("items", [])


class TestFiltrarPorSearch:
    def test_dado_dois_produtos__quando_filtrar_por_search__entao_retorna_apenas_correspondentes(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F01"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"Camiseta Azul {uid}"})
        product_factory({"name": f"Calca Jeans {uid}"})

        # Act
        resp = api.get(products_url, params={"search": f"Camiseta Azul {uid}"})

        # Assert
        assert resp.status_code == 200
        results = _items(resp)
        assert len(results) == 1
        assert results[0]["name"] == f"Camiseta Azul {uid}"


class TestFiltrarPorStatus:
    def test_dado_produto_published__quando_filtrar_status_published__entao_retorna_apenas_published(
        self, api: ApiClient, products_url: str, product_factory, category_id: int
    ):
        """E2E-F02 — published requires description + at least one categoryId"""
        # Arrange
        uid = _uid()
        product_factory({
            "name": f"Published {uid}",
            "description": f"Descricao published {uid}",
            "status": "published",
            "categoryIds": [category_id],
        })
        product_factory({"name": f"Draft {uid}", "status": "draft"})

        # Act
        resp = api.get(products_url, params={"search": uid, "status": "published"})

        # Assert
        assert resp.status_code == 200
        results = _items(resp)
        assert len(results) == 1
        assert results[0]["status"] == "published"

    def test_dado_produto_draft__quando_filtrar_status_draft__entao_retorna_apenas_draft(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F03 — uses inactive as counter-product (no extra validation required)"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"Draft {uid}", "status": "draft"})
        product_factory({"name": f"Inactive {uid}", "status": "inactive"})

        # Act
        resp = api.get(products_url, params={"search": uid, "status": "draft"})

        # Assert
        assert resp.status_code == 200
        results = _items(resp)
        assert len(results) == 1
        assert results[0]["status"] == "draft"


class TestFiltrarPorEstoque:
    def test_dado_produto_com_stock__quando_filtrar_inStock_true__entao_retorna_apenas_com_estoque(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F04"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"ComEstoque {uid}", "stockQuantity": 5})
        product_factory({"name": f"SemEstoque {uid}", "stockQuantity": 0})

        # Act
        resp = api.get(products_url, params={"search": uid, "inStock": "true"})

        # Assert
        assert resp.status_code == 200
        results = _items(resp)
        assert len(results) == 1
        assert results[0]["stockQuantity"] > 0

    def test_dado_produto_sem_stock__quando_filtrar_inStock_false__entao_retorna_apenas_sem_estoque(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F05"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"ComEstoque {uid}", "stockQuantity": 5})
        product_factory({"name": f"SemEstoque {uid}", "stockQuantity": 0})

        # Act
        resp = api.get(products_url, params={"search": uid, "inStock": "false"})

        # Assert
        assert resp.status_code == 200
        results = _items(resp)
        assert len(results) == 1
        assert results[0]["stockQuantity"] == 0


class TestOrdenacao:
    def test_dado_varios_produtos__quando_ordenar_por_nome__entao_retorna_ordem_alfabetica(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F06"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"Zebra {uid}"})
        product_factory({"name": f"Abacate {uid}"})
        product_factory({"name": f"Maca {uid}"})

        # Act
        resp = api.get(products_url, params={"search": uid, "sort": "name"})

        # Assert
        assert resp.status_code == 200
        names = [p["name"] for p in _items(resp)]
        assert names == sorted(names, key=str.lower)

    def test_dado_varios_produtos__quando_ordenar_por_preco__entao_retorna_ordem_crescente(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F07"""
        # Arrange
        uid = _uid()
        product_factory({"name": f"Caro {uid}", "price": 300.0})
        product_factory({"name": f"Barato {uid}", "price": 10.0})
        product_factory({"name": f"Medio {uid}", "price": 150.0})

        # Act
        resp = api.get(products_url, params={"search": uid, "sort": "price"})

        # Assert
        assert resp.status_code == 200
        prices = [p["price"] for p in _items(resp)]
        assert prices == sorted(prices)


class TestPaginacao:
    def test_dado_cinco_produtos__quando_solicitar_pagina_1_pageSize_2__entao_retorna_2_itens(
        self, api: ApiClient, products_url: str, product_factory
    ):
        """E2E-F08"""
        # Arrange
        uid = _uid()
        for i in range(5):
            product_factory({"name": f"Paginacao {uid} Item {i}"})

        # Act
        resp = api.get(
            products_url,
            params={"search": uid, "page": 1, "pageSize": 2},
        )

        # Assert
        assert resp.status_code == 200
        assert len(_items(resp)) == 2
