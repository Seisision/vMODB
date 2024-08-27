package dk.ku.di.dms.vms.marketplace.product;

import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.util.Date;

@VmsTable(name="products")
@IdClass(Product.ProductId.class)
public final class Product implements IEntity<Product.ProductId> {

    public static class ProductId implements Serializable {
        public int seller_id;
        public int product_id;

        @SuppressWarnings("unused")
        public ProductId(){}

        public ProductId(int seller_id, int product_id) {
            this.seller_id = seller_id;
            this.product_id = product_id;
        }
    }

    @Id
    public int seller_id;

    @Id
    public int product_id;

    @Column
    public String name;

    @Column
    public String sku;

    @Column
    public String category;

    @Column
    public String description;

    @Column
    public float price;

    @Column
    public float freight_value;

    @Column
    public String status;

    @Column
    public String version;

    @Column
    public Date created_at;

    @Column
    public Date updated_at;

    public Product(){}

    public Product(int seller_id, int product_id, String name, String sku, String category,
                   String description, float price, float freight_value, String status, String version) {
        this.seller_id = seller_id;
        this.product_id = product_id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.description = description;
        this.price = price;
        this.freight_value = freight_value;
        this.status = status;
        this.version = version;
    }

    @Override
    public String toString() {
        return "{"
                + "\"seller_id\":\"" + seller_id + "\""
                + ",\"product_id\":\"" + product_id + "\""
                + ",\"name\":\"" + name + "\""
                + ",\"sku\":\"" + sku + "\""
                + ",\"category\":\"" + category + "\""
                + ",\"description\":\"" + description + "\""
                + ",\"price\":\"" + price + "\""
                + ",\"freight_value\":\"" + freight_value + "\""
                + ",\"status\":\"" + status + "\""
                + ",\"version\":\"" + version + "\""
                + "}";
    }

}
