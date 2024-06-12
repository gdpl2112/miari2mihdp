package clinet.mihdp;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author github.kloping
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class ResDataPack {
    private String action;

    private String id;
    private String bot_id;
    private String env_type;
    private String env_id = "";
    private GeneralData data;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
