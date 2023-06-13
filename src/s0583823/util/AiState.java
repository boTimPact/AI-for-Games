package s0583823.util;

import java.awt.geom.Point2D;

public abstract class AiState {

    public abstract Point2D getNextAction();

    public abstract void switchState();

}
