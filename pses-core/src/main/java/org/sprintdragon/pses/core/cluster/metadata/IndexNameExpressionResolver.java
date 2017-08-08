package org.sprintdragon.pses.core.cluster.metadata;

import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.common.component.AbstractComponent;
import org.sprintdragon.pses.core.common.settings.Settings;

import javax.annotation.Resource;

/**
 * Created by wangdi on 17-8-8.
 */
@Component
public class IndexNameExpressionResolver extends AbstractComponent {
    public IndexNameExpressionResolver(Settings settings) {
        super(settings);
    }
}
