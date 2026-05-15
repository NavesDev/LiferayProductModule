# Test Coverage Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir a suite de testes do módulo Product para estar em conformidade com o padrão `liferay-unit-tests` da Sea Tecnologia — substituindo injeção manual por `@InjectMocks`, adicionando JaCoCo com thresholds obrigatórios, e cobrindo os métodos de negócio sem testes.

**Architecture:** Os dois módulos de teste (`product-service-service` e `product-rest-impl`) usam JUnit 5 + Mockito 5. A injeção de dependências hoje é feita via reflection manual em `@BeforeEach`; vamos substituir por `@InjectMocks`. JaCoCo será adicionado diretamente nos `build.gradle` de cada módulo (sem arquivo compartilhado). Os novos cenários seguem o padrão `dado_X_quando_Y_entao_Z` com `@Nested` por método.

**Tech Stack:** JUnit Jupiter 5.10.2, Mockito 5.12.0 (mockito-junit-jupiter), AssertJ 3.25.3, JaCoCo (Gradle plugin `jacoco`), Liferay 7.4 CE

---

## File Map

| Ação | Arquivo |
|------|---------|
| Modify | `modules/product-service/product-service-service/src/test/java/product/service/service/impl/ProductLocalServiceImplTest.java` |
| Modify | `modules/product-rest/product-rest-impl/src/test/java/product/rest/internal/resource/v1_0/ProductResourceImplTest.java` |
| Modify | `modules/product-service/product-service-service/build.gradle` |
| Modify | `modules/product-rest/product-rest-impl/build.gradle` |

---

## Task 1: Substituir injeção manual por `@InjectMocks` em `ProductLocalServiceImplTest`

**Files:**
- Modify: `modules/product-service/product-service-service/src/test/java/product/service/service/impl/ProductLocalServiceImplTest.java`

O `ProductLocalServiceImpl` estende `ProductLocalServiceBaseImpl`, que injeta `productPersistence`, `counterLocalService` e `assetEntryLocalService` via `@Reference`. O Mockito 5.x com `@InjectMocks` reflete em campos privados e com underscore (`_field`) da hierarquia completa — desde que os nomes correspondam. O `productPersistence` no `BaseImpl` se chama `productPersistence` (sem underscore), portanto `@InjectMocks` funciona diretamente.

- [ ] **Step 1: Remover o `@BeforeEach` com reflection manual e a helper `_setField`**

Substituir o conteúdo inteiro do arquivo por:

```java
package product.service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import product.service.exception.ProductCategoryException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductValidationException;
import product.service.model.Product;
import product.service.model.ProductStatusConstants;
import product.service.model.impl.ProductImpl;
import product.service.service.persistence.ProductPersistence;

@DisplayName("ProductLocalServiceImpl")
@ExtendWith(MockitoExtension.class)
class ProductLocalServiceImplTest {

    private static final long CATEGORY_ID = 301L;
    private static final long COMPANY_ID = 10001L;
    private static final String DESCRIPTION = "Descricao do produto";
    private static final long GROUP_ID = 30001L;
    private static final String NAME = "Produto teste";
    private static final double PRICE = 99.90D;
    private static final long PRODUCT_ID = 40001L;
    private static final int STOCK_QUANTITY = 10;
    private static final long TAG_ID = 501L;
    private static final long USER_ID = 20001L;
    private static final String USER_NAME = "Test User";

    @Mock
    private AssetCategoryLocalService assetCategoryLocalService;

    @Mock
    private AssetEntryLocalService assetEntryLocalService;

    @Mock
    private AssetTagLocalService assetTagLocalService;

    @Mock
    private CounterLocalService counterLocalService;

    @Mock
    private ProductPersistence productPersistence;

    @Mock
    private ResourceLocalService resourceLocalService;

    @Mock
    private UserLocalService userLocalService;

    @Captor
    private ArgumentCaptor<Product> productArgumentCaptor;

    @InjectMocks
    private ProductLocalServiceImpl productLocalService;

    @BeforeEach
    void setUp() {
        assertThat(productLocalService).isNotNull();
    }

    @Nested
    @DisplayName("Adicionar produto")
    class AddProduct {

        @Test
        @DisplayName("Dado draft valido, quando adicionar, entao persiste produto e sincroniza asset")
        void dado_draftValido_quando_adicionar_entao_persisteProdutoESincronizaAsset()
            throws Exception {

            // Arrange
            ServiceContext serviceContext = _serviceContext();
            Product createdProduct = new ProductImpl();
            createdProduct.setProductId(PRODUCT_ID);

            when(counterLocalService.increment(Product.class.getName())).thenReturn(PRODUCT_ID);
            when(productPersistence.create(PRODUCT_ID)).thenReturn(createdProduct);
            when(productPersistence.update(any(Product.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
            when(userLocalService.getUser(USER_ID)).thenReturn(_mockUser());
            AssetCategory category = _mockCategory(GROUP_ID);
            when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(category);
            AssetTag tag = _mockTag(GROUP_ID, "tag-1");
            when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(tag);

            // Act
            Product product = productLocalService.addProduct(
                USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
                ProductStatusConstants.DRAFT, STOCK_QUANTITY,
                new long[] {CATEGORY_ID}, new long[] {TAG_ID}, serviceContext);

            // Assert
            assertThat(product.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(product.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
            assertThat(product.getStockQuantity()).isEqualTo(STOCK_QUANTITY);

            verify(productPersistence).update(productArgumentCaptor.capture());
            Product persisted = productArgumentCaptor.getValue();
            assertThat(persisted.getName()).isEqualTo(NAME);
            assertThat(persisted.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(persisted.getPrice()).isEqualTo(PRICE);
            assertThat(persisted.getUserName()).isEqualTo(USER_NAME);

            verify(assetEntryLocalService).updateEntry(
                anyLong(), anyLong(), any(Date.class), any(Date.class),
                anyString(), anyLong(), anyString(), anyLong(),
                aryEq(new long[] {CATEGORY_ID}), aryEq(new String[] {"tag-1"}),
                anyBoolean(), anyBoolean(), nullable(Date.class),
                nullable(Date.class), nullable(Date.class), nullable(Date.class),
                nullable(String.class), anyString(), anyString(),
                nullable(String.class), nullable(String.class),
                nullable(String.class), anyInt(), anyInt(), anyDouble(),
                any(ServiceContext.class));
        }

        @Test
        @DisplayName("Dado published sem categorias, quando adicionar, entao bloqueia publicacao")
        void dado_publishedSemCategorias_quando_adicionar_entao_bloqueiaPublicacao() {
            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.addProduct(
                    USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
                    ProductStatusConstants.PUBLISHED, STOCK_QUANTITY, new long[0],
                    new long[0], _serviceContext())
            ).isInstanceOf(ProductValidationException.class);
        }

        @Test
        @DisplayName("Dado estoque negativo, quando adicionar, entao rejeita")
        void dado_estoqueNegativo_quando_adicionar_entao_rejeita() {
            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.addProduct(
                    USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
                    ProductStatusConstants.DRAFT, -1, new long[0], new long[0],
                    _serviceContext())
            ).isInstanceOf(ProductValidationException.class);
        }

        @Test
        @DisplayName("Dado categoria de outro grupo, quando adicionar, entao rejeita")
        void dado_categoriaDeOutroGrupo_quando_adicionar_entao_rejeita()
            throws Exception {

            // Arrange
            AssetCategory category = _mockCategory(99999L);
            when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(category);

            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.addProduct(
                    USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
                    ProductStatusConstants.DRAFT, STOCK_QUANTITY,
                    new long[] {CATEGORY_ID}, new long[0], _serviceContext())
            ).isInstanceOf(ProductCategoryException.class);
        }
    }

    @Nested
    @DisplayName("Atualizar status")
    class UpdateProductStatus {

        @Test
        @DisplayName("Dado draft valido com categoria, quando publicar, entao atualiza status e sincroniza asset")
        void dado_draftValidoComCategoria_quando_publicar_entao_atualizaStatusESincronizaAsset()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.DRAFT);
            AssetEntry assetEntry = _mockAssetEntry(new long[] {CATEGORY_ID}, new String[] {"tag-1"});

            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
            when(productPersistence.update(any(Product.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
            when(assetEntryLocalService.getEntry(
                Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);

            // Act
            Product updated = productLocalService.updateProductStatus(
                PRODUCT_ID, ProductStatusConstants.PUBLISHED, _serviceContext());

            // Assert
            assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.PUBLISHED);

            verify(productPersistence).update(productArgumentCaptor.capture());
            assertThat(productArgumentCaptor.getValue().getStatus())
                .isEqualTo(ProductStatusConstants.PUBLISHED);
        }

        @Test
        @DisplayName("Dado published para draft, quando atualizar status, entao rejeita transicao")
        void dado_publishedParaDraft_quando_atualizarStatus_entao_rejeitaTransicao()
            throws Exception {

            // Arrange
            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(
                _product(ProductStatusConstants.PUBLISHED));

            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.updateProductStatus(
                    PRODUCT_ID, ProductStatusConstants.DRAFT, _serviceContext())
            ).isInstanceOf(ProductStatusException.class);
        }
    }

    @Nested
    @DisplayName("Remover produto")
    class DeleteProduct {

        @Test
        @DisplayName("Dado produto existente, quando remover, entao remove asset e persiste exclusao")
        void dado_produtoExistente_quando_remover_entao_removeAssetEPersisteExclusao()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.DRAFT);
            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
            when(productPersistence.remove(any(Product.class))).thenReturn(product);

            // Act
            Product deleted = productLocalService.deleteProduct(PRODUCT_ID);

            // Assert
            assertThat(deleted).isSameAs(product);
            verify(assetEntryLocalService).deleteEntry(Product.class.getName(), PRODUCT_ID);
            verify(productPersistence).remove(product);
        }
    }

    @Nested
    @DisplayName("Buscar produtos por grupo")
    class GetProductsByGroupId {

        @Test
        @DisplayName("Dado grupo com produtos, quando buscar, entao retorna lista do grupo")
        void dado_grupoComProdutos_quando_buscar_entao_retornaListaDoGrupo() {
            // Arrange
            Product p1 = _product(ProductStatusConstants.DRAFT);
            Product p2 = _product(ProductStatusConstants.PUBLISHED);
            when(productPersistence.findByGroupId(GROUP_ID)).thenReturn(List.of(p1, p2));

            // Act
            List<Product> result = productLocalService.getProductsByGroupId(GROUP_ID);

            // Assert
            assertThat(result).hasSize(2).containsExactly(p1, p2);
            verify(productPersistence).findByGroupId(GROUP_ID);
        }

        @Test
        @DisplayName("Dado grupo sem produtos, quando buscar, entao retorna lista vazia")
        void dado_grupoSemProdutos_quando_buscar_entao_retornaListaVazia() {
            // Arrange
            when(productPersistence.findByGroupId(GROUP_ID)).thenReturn(List.of());

            // Act
            List<Product> result = productLocalService.getProductsByGroupId(GROUP_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Atualizar categorias")
    class UpdateProductCategories {

        @Test
        @DisplayName("Dado produto draft com categoria valida, quando atualizar, entao sincroniza asset")
        void dado_produtoDraftComCategoriaValida_quando_atualizar_entao_sincronizaAsset()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.DRAFT);
            AssetEntry assetEntry = _mockAssetEntry(new long[0], new String[] {"tag-existente"});
            AssetCategory category = _mockCategory(GROUP_ID);

            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
            when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(category);
            when(assetEntryLocalService.getEntry(
                Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);

            // Act
            Product result = productLocalService.updateProductCategories(
                PRODUCT_ID, new long[] {CATEGORY_ID}, _serviceContext());

            // Assert
            assertThat(result).isSameAs(product);
            verify(assetEntryLocalService).updateEntry(
                anyLong(), anyLong(), any(Date.class), any(Date.class),
                anyString(), anyLong(), anyString(), anyLong(),
                aryEq(new long[] {CATEGORY_ID}), any(String[].class),
                anyBoolean(), anyBoolean(), nullable(Date.class),
                nullable(Date.class), nullable(Date.class), nullable(Date.class),
                nullable(String.class), anyString(), anyString(),
                nullable(String.class), nullable(String.class),
                nullable(String.class), anyInt(), anyInt(), anyDouble(),
                any(ServiceContext.class));
        }

        @Test
        @DisplayName("Dado produto published sem categorias, quando atualizar, entao rejeita")
        void dado_produtoPublishedSemCategorias_quando_atualizar_entao_rejeita()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.PUBLISHED);
            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);

            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.updateProductCategories(
                    PRODUCT_ID, new long[0], _serviceContext())
            ).isInstanceOf(ProductValidationException.class);
        }
    }

    @Nested
    @DisplayName("Atualizar tags")
    class UpdateProductTags {

        @Test
        @DisplayName("Dado produto com tags validas, quando atualizar, entao sincroniza asset com novas tags")
        void dado_produtoComTagsValidas_quando_atualizar_entao_sincronizaAssetComNovasTags()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.DRAFT);
            AssetEntry assetEntry = _mockAssetEntry(new long[] {CATEGORY_ID}, new String[0]);
            AssetTag tag = _mockTag(GROUP_ID, "nova-tag");

            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
            when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(tag);
            when(assetEntryLocalService.getEntry(
                Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);

            // Act
            Product result = productLocalService.updateProductTags(
                PRODUCT_ID, new long[] {TAG_ID}, _serviceContext());

            // Assert
            assertThat(result).isSameAs(product);
            verify(assetEntryLocalService).updateEntry(
                anyLong(), anyLong(), any(Date.class), any(Date.class),
                anyString(), anyLong(), anyString(), anyLong(),
                any(long[].class), aryEq(new String[] {"nova-tag"}),
                anyBoolean(), anyBoolean(), nullable(Date.class),
                nullable(Date.class), nullable(Date.class), nullable(Date.class),
                nullable(String.class), anyString(), anyString(),
                nullable(String.class), nullable(String.class),
                nullable(String.class), anyInt(), anyInt(), anyDouble(),
                any(ServiceContext.class));
        }

        @Test
        @DisplayName("Dado tag de outro grupo, quando atualizar, entao rejeita")
        void dado_tagDeOutroGrupo_quando_atualizar_entao_rejeita()
            throws Exception {

            // Arrange
            Product product = _product(ProductStatusConstants.DRAFT);
            AssetTag tag = _mockTag(99999L, "tag-errada");

            when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
            when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(tag);

            // Act / Assert
            assertThatThrownBy(
                () -> productLocalService.updateProductTags(
                    PRODUCT_ID, new long[] {TAG_ID}, _serviceContext())
            ).isInstanceOf(product.service.exception.ProductTagException.class);
        }
    }

    // ---- helpers ----

    private User _mockUser() {
        com.liferay.portal.kernel.model.User user =
            org.mockito.Mockito.mock(com.liferay.portal.kernel.model.User.class);
        when(user.getCompanyId()).thenReturn(COMPANY_ID);
        when(user.getFullName()).thenReturn(USER_NAME);
        return user;
    }

    private AssetCategory _mockCategory(long groupId) {
        AssetCategory category = org.mockito.Mockito.mock(AssetCategory.class);
        when(category.getGroupId()).thenReturn(groupId);
        return category;
    }

    private AssetTag _mockTag(long groupId, String name) {
        AssetTag tag = org.mockito.Mockito.mock(AssetTag.class);
        when(tag.getGroupId()).thenReturn(groupId);
        when(tag.getName()).thenReturn(name);
        return tag;
    }

    private AssetEntry _mockAssetEntry(long[] categoryIds, String[] tagNames) {
        AssetEntry entry = org.mockito.Mockito.mock(AssetEntry.class);
        when(entry.getCategoryIds()).thenReturn(categoryIds);
        when(entry.getTagNames()).thenReturn(tagNames);
        return entry;
    }

    private Product _product(int status) {
        Product product = new ProductImpl();
        product.setCompanyId(COMPANY_ID);
        product.setCreateDate(new Date());
        product.setDescription(DESCRIPTION);
        product.setGroupId(GROUP_ID);
        product.setModifiedDate(new Date());
        product.setName(NAME);
        product.setPrice(PRICE);
        product.setProductId(PRODUCT_ID);
        product.setStatus(status);
        product.setStockQuantity(STOCK_QUANTITY);
        product.setUserId(USER_ID);
        product.setUserName(USER_NAME);
        product.setUuid("product-uuid");
        return product;
    }

    private ServiceContext _serviceContext() {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(COMPANY_ID);
        serviceContext.setScopeGroupId(GROUP_ID);
        serviceContext.setUserId(USER_ID);
        return serviceContext;
    }

}
```

- [ ] **Step 2: Rodar os testes para verificar que compilam e passam**

```bash
cd /home/naves/LiferayWorkspaces/LiferayProductModule
blade gw :modules:product-service:product-service-service:test --tests "product.service.service.impl.ProductLocalServiceImplTest" 2>&1 | tail -20
```

Esperado: `BUILD SUCCESSFUL` com todos os testes passando.

> **Troubleshooting:** Se `@InjectMocks` falhar silenciosamente (campo `null`), o `assertThat(productLocalService).isNotNull()` no `@BeforeEach` vai expor o problema. Nesse caso, checar se os nomes dos campos `@Mock` batem com os nomes dos campos `@Reference` em `ProductLocalServiceBaseImpl` (gerado pelo Service Builder).

- [ ] **Step 3: Commit**

```bash
git add modules/product-service/product-service-service/src/test/java/product/service/service/impl/ProductLocalServiceImplTest.java
git commit -m "test(product-service): replace manual reflection injection with @InjectMocks and add missing test scenarios"
```

---

## Task 2: Substituir injeção manual por `@InjectMocks` em `ProductResourceImplTest` e adicionar cenários faltantes

**Files:**
- Modify: `modules/product-rest/product-rest-impl/src/test/java/product/rest/internal/resource/v1_0/ProductResourceImplTest.java`

`ProductResourceImpl` tem três `@Reference` com prefixo underscore: `_assetEntryLocalService`, `_assetTagLocalService`, `_productLocalService`. O Mockito 5.x injeta por nome de campo, incluindo underscore. Porém `contextCompany` e `contextUser` são setados via setters herdados de `BaseProductResourceImpl` — esses continuam sendo configurados no `@BeforeEach` via `setContextCompany` / `setContextUser`.

Adicionar também os cenários faltantes: produto não encontrado (`NotFoundException`) via `_validateProductSite`, e payload nulo (`BadRequestException`).

- [ ] **Step 1: Reescrever o arquivo com `@InjectMocks` e novos cenários**

```java
package product.rest.internal.resource.v1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.portal.kernel.exception.NoSuchModelException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.vulcan.pagination.Page;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import product.rest.dto.v1_0.Product;
import product.rest.dto.v1_0.ProductCategories;
import product.rest.dto.v1_0.ProductTags;
import product.service.model.impl.ProductImpl;
import product.service.service.ProductLocalService;

@DisplayName("ProductResourceImpl")
@ExtendWith(MockitoExtension.class)
class ProductResourceImplTest {

    private static final long COMPANY_ID = 10001L;
    private static final long GROUP_ID = 30001L;
    private static final long PRODUCT_ID = 40001L;
    private static final long SITE_ID = 30001L;
    private static final long USER_ID = 20001L;

    @Mock
    private AssetEntry assetEntry;

    @Mock
    private AssetEntryLocalService _assetEntryLocalService;

    @Mock
    private AssetTag assetTag;

    @Mock
    private AssetTagLocalService _assetTagLocalService;

    @Mock
    private Company company;

    @Mock
    private ProductLocalService _productLocalService;

    @Mock
    private User user;

    @Captor
    private ArgumentCaptor<ServiceContext> serviceContextCaptor;

    @Captor
    private ArgumentCaptor<long[]> categoryIdsCaptor;

    @InjectMocks
    private ProductResourceImpl productResource;

    @BeforeEach
    void setUp() {
        assertThat(productResource).isNotNull();

        productResource.setContextCompany(company);
        productResource.setContextUser(user);

        lenient().when(company.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(user.getUserId()).thenReturn(USER_ID);
        lenient().when(user.getGroupId()).thenReturn(GROUP_ID);
    }

    @Nested
    @DisplayName("Criar produto")
    class PostProduct {

        @Test
        @DisplayName("Dado payload valido, quando criar, entao delega ao local service e retorna DTO")
        void dado_payloadValido_quando_criar_entao_delegaAoLocalServiceERetornaDto()
            throws Exception {

            // Arrange
            Product payload = new Product();
            payload.setName("Produto A");
            payload.setDescription("Descricao A");
            payload.setPrice(19.9D);
            payload.setStatus(Product.Status.DRAFT);
            payload.setStockQuantity(7);
            payload.setCategoryIds(new Long[] {11L});
            payload.setTagIds(new Long[] {22L});

            ProductImpl created = _product(PRODUCT_ID, "Produto A", 7);

            when(_productLocalService.addProduct(
                anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
                anyInt(), anyInt(), any(long[].class), any(long[].class),
                any(ServiceContext.class))
            ).thenReturn(created);
            when(_assetEntryLocalService.getEntry(
                product.service.model.Product.class.getName(), PRODUCT_ID)
            ).thenReturn(assetEntry);
            when(assetEntry.getCategoryIds()).thenReturn(new long[] {11L});
            when(assetEntry.getTagNames()).thenReturn(new String[] {"tag-22"});
            when(_assetTagLocalService.fetchTag(GROUP_ID, "tag-22")).thenReturn(assetTag);
            when(assetTag.getTagId()).thenReturn(22L);

            // Act
            Product response = productResource.postSiteProduct(SITE_ID, payload);

            // Assert
            assertThat(response.getId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getName()).isEqualTo("Produto A");
            assertThat(response.getCategoryIds()).containsExactly(11L);
            assertThat(response.getTagIds()).containsExactly(22L);

            verify(_productLocalService).addProduct(
                anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
                anyInt(), anyInt(), any(long[].class), any(long[].class),
                serviceContextCaptor.capture());
            assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
            assertThat(serviceContextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Dado payload nulo, quando criar, entao lanca BadRequestException")
        void dado_payloadNulo_quando_criar_entao_lancaBadRequestException() {
            // Act / Assert
            assertThatThrownBy(
                () -> productResource.postSiteProduct(SITE_ID, null)
            ).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Consultar produto por ID")
    class GetSiteProduct {

        @Test
        @DisplayName("Dado produto inexistente no site, quando buscar, entao lanca BadRequestException")
        void dado_produtoInexistenteNoSite_quando_buscar_entao_lancaBadRequestException()
            throws Exception {

            // Arrange
            ProductImpl productOutroSite = _product(PRODUCT_ID, "Produto X", 1);
            productOutroSite.setGroupId(99999L); // diferente de SITE_ID

            when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(productOutroSite);

            // Act / Assert
            assertThatThrownBy(
                () -> productResource.getSiteProduct(SITE_ID, PRODUCT_ID)
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("siteId");
        }
    }

    @Nested
    @DisplayName("Consultar produtos")
    class GetProducts {

        @Test
        @DisplayName("Dado filtro status, quando listar, entao retorna apenas produtos publicados")
        void dado_filtroStatus_quando_listar_entao_retornaApenasProdutosPublicados()
            throws Exception {

            // Arrange
            ProductImpl published = _product(1L, "Notebook", 5);
            published.setStatus(1);

            ProductImpl draft = _product(2L, "Mouse", 0);
            draft.setStatus(0);

            when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
                List.of(published, draft));
            when(_assetEntryLocalService.getEntry(
                product.service.model.Product.class.getName(), 1L)
            ).thenReturn(assetEntry);
            when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
            when(assetEntry.getTagNames()).thenReturn(new String[0]);
            when(_assetEntryLocalService.getEntry(
                product.service.model.Product.class.getName(), 2L)
            ).thenReturn(assetEntry);

            // Act
            Page<Product> page = productResource.getSiteProductsPage(
                SITE_ID, null, "published", null, null, null, null, null);

            // Assert
            assertThat(page.getItems()).hasSize(1);
            assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Atualizar classificacao")
    class UpdateClassification {

        @Test
        @DisplayName("Dado categorias validas, quando atualizar categorias, entao delega com IDs corretos e retorna DTO")
        void dado_categoriasValidas_quando_atualizarCategorias_entao_delegaComIdsCorretosERetornaDto()
            throws Exception {

            // Arrange
            ProductCategories productCategories = new ProductCategories();
            productCategories.setCategoryIds(new Long[] {9L, 10L});

            ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);
            updated.setGroupId(SITE_ID);

            when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);
            when(_productLocalService.updateProductCategories(
                anyLong(), any(), any(ServiceContext.class))
            ).thenReturn(updated);
            when(_assetEntryLocalService.getEntry(
                product.service.model.Product.class.getName(), PRODUCT_ID)
            ).thenReturn(assetEntry);
            when(assetEntry.getCategoryIds()).thenReturn(new long[] {9L, 10L});
            when(assetEntry.getTagNames()).thenReturn(new String[0]);

            // Act
            Product response = productResource.putSiteProductCategories(
                SITE_ID, PRODUCT_ID, productCategories);

            // Assert
            assertThat(response.getId()).isEqualTo(PRODUCT_ID);

            verify(_productLocalService).updateProductCategories(
                eq(PRODUCT_ID), categoryIdsCaptor.capture(), serviceContextCaptor.capture());
            assertThat(categoryIdsCaptor.getValue()).containsExactly(9L, 10L);
            assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
        }

        @Test
        @DisplayName("Dado payload nulo, quando atualizar categorias, entao lanca BadRequestException")
        void dado_payloadNulo_quando_atualizarCategorias_entao_lancaBadRequestException() {
            // Act / Assert
            assertThatThrownBy(
                () -> productResource.putSiteProductCategories(SITE_ID, PRODUCT_ID, null)
            ).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Dado tags validas, quando atualizar tags, entao delega e retorna DTO")
        void dado_tagsValidas_quando_atualizarTags_entao_delegaERetornaDto()
            throws Exception {

            // Arrange
            ProductTags productTags = new ProductTags();
            productTags.setTagIds(new Long[] {7L});

            ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);
            updated.setGroupId(SITE_ID);

            when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);
            when(_productLocalService.updateProductTags(
                anyLong(), any(), any(ServiceContext.class))
            ).thenReturn(updated);
            when(_assetEntryLocalService.getEntry(
                product.service.model.Product.class.getName(), PRODUCT_ID)
            ).thenReturn(assetEntry);
            when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
            when(assetEntry.getTagNames()).thenReturn(new String[0]);

            // Act
            Product response = productResource.putSiteProductTags(
                SITE_ID, PRODUCT_ID, productTags);

            // Assert
            assertThat(response.getId()).isEqualTo(PRODUCT_ID);
            verify(_productLocalService).updateProductTags(
                eq(PRODUCT_ID), any(long[].class), serviceContextCaptor.capture());
            assertThat(serviceContextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Dado payload nulo, quando atualizar tags, entao lanca BadRequestException")
        void dado_payloadNulo_quando_atualizarTags_entao_lancaBadRequestException() {
            // Act / Assert
            assertThatThrownBy(
                () -> productResource.putSiteProductTags(SITE_ID, PRODUCT_ID, null)
            ).isInstanceOf(BadRequestException.class);
        }
    }

    // ---- helpers ----

    private ProductImpl _product(long id, String name, int stockQuantity) {
        ProductImpl product = new ProductImpl();
        product.setProductId(id);
        product.setDescription("Descricao");
        product.setGroupId(SITE_ID);
        product.setName(name);
        product.setPrice(10.5D);
        product.setStatus(0);
        product.setStockQuantity(stockQuantity);
        return product;
    }

}
```

- [ ] **Step 2: Rodar os testes**

```bash
blade gw :modules:product-rest:product-rest-impl:test --tests "product.rest.internal.resource.v1_0.ProductResourceImplTest" 2>&1 | tail -20
```

Esperado: `BUILD SUCCESSFUL`.

> **Troubleshooting — campos `_assetEntryLocalService` não injetados:** Se o `@InjectMocks` falhar porque `BaseProductResourceImpl` é gerado e os setters não são acessíveis, usar `@BeforeEach` + reflection só para os campos com underscore que o Mockito não conseguir injetar:
> ```java
> _setField(productResource, "_assetEntryLocalService", _assetEntryLocalService);
> _setField(productResource, "_assetTagLocalService", _assetTagLocalService);
> ```
> Nesse caso, manter `@InjectMocks` para `_productLocalService` e injetar manualmente apenas o que falhar.

- [ ] **Step 3: Commit**

```bash
git add modules/product-rest/product-rest-impl/src/test/java/product/rest/internal/resource/v1_0/ProductResourceImplTest.java
git commit -m "test(product-rest): replace manual reflection injection with @InjectMocks and add missing error scenarios"
```

---

## Task 3: Adicionar JaCoCo em `product-service-service`

**Files:**
- Modify: `modules/product-service/product-service-service/build.gradle`

O padrão exige ≥ 85% line coverage e ≥ 75% branch coverage para módulos `*-service`. As exclusões são as classes geradas pelo Service Builder (padrão `**/*BaseImpl*`, `**/*ModelImpl*`, `**/*CacheModel*`, `**/*PersistenceImpl*`, `**/*FinderImpl*`).

- [ ] **Step 1: Ler o `build.gradle` atual**

```
modules/product-service/product-service-service/build.gradle
```

Conteúdo atual relevante (seção `test`):

```gradle
test {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
    useJUnitPlatform()
}
```

- [ ] **Step 2: Adicionar o bloco JaCoCo ao `build.gradle`**

Adicionar ao final do arquivo (após o bloco `test { ... }`):

```gradle
apply plugin: 'jacoco'

jacoco {
    toolVersion = '0.8.11'
}

jacocoTestReport {
    dependsOn test

    reports {
        html.required = true
        xml.required = false
    }

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.collect {
                fileTree(dir: it, exclude: [
                    '**/service/base/**',
                    '**/model/impl/**',
                    '**/service/persistence/impl/**',
                ])
            })
        )
    }
}

jacocoTestCoverageVerification {
    dependsOn jacocoTestReport

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.collect {
                fileTree(dir: it, exclude: [
                    '**/service/base/**',
                    '**/model/impl/**',
                    '**/service/persistence/impl/**',
                ])
            })
        )
    }

    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.85
            }
        }
        rule {
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.75
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

- [ ] **Step 3: Gerar o relatório e verificar cobertura**

```bash
blade gw :modules:product-service:product-service-service:jacocoTestReport 2>&1 | tail -30
```

O relatório HTML estará em:
```
modules/product-service/product-service-service/build/reports/jacoco/test/html/index.html
```

- [ ] **Step 4: Rodar a verificação de threshold**

```bash
blade gw :modules:product-service:product-service-service:jacocoTestCoverageVerification 2>&1 | tail -20
```

Esperado: `BUILD SUCCESSFUL`. Se falhar com `Rule violated`, revisar quais métodos não têm cobertura e adicionar testes antes de prosseguir.

- [ ] **Step 5: Commit**

```bash
git add modules/product-service/product-service-service/build.gradle
git commit -m "build(product-service): add JaCoCo with 85%/75% line/branch coverage thresholds"
```

---

## Task 4: Adicionar JaCoCo em `product-rest-impl`

**Files:**
- Modify: `modules/product-rest/product-rest-impl/build.gradle`

Padrão para `*-rest`: ≥ 80% line / ≥ 70% branch. Excluir classes geradas pelo REST Builder (`**/resource/v1_0/Base*`, `**/*DTOImpl*`).

- [ ] **Step 1: Adicionar o bloco JaCoCo ao `build.gradle` de `product-rest-impl`**

Adicionar ao final do arquivo (após o bloco `test { ... }`):

```gradle
apply plugin: 'jacoco'

jacoco {
    toolVersion = '0.8.11'
}

jacocoTestReport {
    dependsOn test

    reports {
        html.required = true
        xml.required = false
    }

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.collect {
                fileTree(dir: it, exclude: [
                    '**/resource/v1_0/Base*',
                    '**/resource/v1_0/*ResourceImpl.class',
                ])
            })
        )
    }
}

jacocoTestCoverageVerification {
    dependsOn jacocoTestReport

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.collect {
                fileTree(dir: it, exclude: [
                    '**/resource/v1_0/Base*',
                ])
            })
        )
    }

    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
        rule {
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.70
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

- [ ] **Step 2: Gerar o relatório**

```bash
blade gw :modules:product-rest:product-rest-impl:jacocoTestReport 2>&1 | tail -30
```

Relatório em:
```
modules/product-rest/product-rest-impl/build/reports/jacoco/test/html/index.html
```

- [ ] **Step 3: Rodar verificação de threshold**

```bash
blade gw :modules:product-rest:product-rest-impl:jacocoTestCoverageVerification 2>&1 | tail -20
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add modules/product-rest/product-rest-impl/build.gradle
git commit -m "build(product-rest): add JaCoCo with 80%/70% line/branch coverage thresholds"
```

---

## Verificação Final

- [ ] **Rodar todos os testes dos dois módulos juntos**

```bash
blade gw :modules:product-service:product-service-service:test :modules:product-rest:product-rest-impl:test 2>&1 | tail -30
```

Esperado: ambos com `BUILD SUCCESSFUL`.

- [ ] **Rodar verificação de cobertura dos dois módulos**

```bash
blade gw :modules:product-service:product-service-service:jacocoTestCoverageVerification :modules:product-rest:product-rest-impl:jacocoTestCoverageVerification 2>&1 | tail -30
```

Esperado: `BUILD SUCCESSFUL` para ambos.

---

## Self-Review

**Cobertura do spec:**
- [x] Task 1: `@InjectMocks` em `ProductLocalServiceImplTest`
- [x] Task 1: `getProductsByGroupId` com happy path + lista vazia
- [x] Task 1: `updateProductCategories` com válido + published sem categoria
- [x] Task 1: `updateProductTags` com válido + tag de outro grupo
- [x] Task 1: `updateProductStatus` happy path (draft → published) adicionado
- [x] Task 2: `@InjectMocks` em `ProductResourceImplTest`
- [x] Task 2: cenários `BadRequestException` para payload nulo em post/put
- [x] Task 2: cenário produto não pertence ao site (`BadRequestException`)
- [x] Task 2: `ArgumentCaptor` com asserção nos campos capturados (AP-01 corrigido)
- [x] Task 3: JaCoCo em `product-service-service` (85%/75%)
- [x] Task 4: JaCoCo em `product-rest-impl` (80%/70%)

**Checklist anti-patterns:**
- Sem `assertEquals`/`assertTrue` — apenas `assertThat`
- Sem `try/catch` em testes — apenas `assertThatThrownBy`
- Sem `if`/`for` dentro de `@Test`
- Sem `Thread.sleep`
- Sem mock do SUT
- `@InjectMocks` declarado por último no bloco de campos
- `ServiceContext` sempre com `companyId > 0` e `userId > 0`
- Todo `verify` com `ArgumentCaptor` + asserção de campo
