package dk.ku.di.dms.vms.tpcc.common.events;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Event
public final class NewOrderWareOut {

    public int w_id;
    public int d_id;
    public int c_id;
    public int[] itemsIds;
    public int[] supWares;
    public int[] qty;
    public boolean allLocal;
    // from warehouse
    public double w_tax;
    public int d_next_o_id;
    public double d_tax;
    public float c_discount;

    public NewOrderWareOut(){}

    public NewOrderWareOut(int w_id, int d_id, int c_id, int[] itemsIds, int[] supWares, int[] qty, boolean allLocal, double w_tax, int d_next_o_id, double d_tax, float c_discount) {
        this.w_id = w_id;
        this.d_id = d_id;
        this.c_id = c_id;
        this.itemsIds = itemsIds;
        this.supWares = supWares;
        this.qty = qty;
        this.allLocal = allLocal;
        this.w_tax = w_tax;
        this.d_next_o_id = d_next_o_id;
        this.d_tax = d_tax;
        this.c_discount = c_discount;
    }

    @SuppressWarnings("unused")
    public List<Integer> getId(){
        if(this.allLocal) return List.of(this.w_id);
        Set<Integer> set = new HashSet<>(4);
        List<Integer> list = new ArrayList<>(4);
        for (int supWare : this.supWares) {
            if (set.add(supWare)) {
                list.add(supWare);
            }
        }
        return list;
    }

}