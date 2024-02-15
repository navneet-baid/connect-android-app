package in.app.connect;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private ScaleGestureDetector scaleGestureDetector;
    private Matrix matrix = new Matrix();
    private PointF lastTouch = new PointF();
    private float scaleFactor = 1f;
    private float focusX, focusY;
    private int viewWidth, viewHeight;
    private int imageWidth, imageHeight;
    private float originalImageScale;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        PointF curr = new PointF(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(curr);
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = curr.x - lastTouch.x;
                float deltaY = curr.y - lastTouch.y;
                lastTouch.set(curr);

                float scaleWidth = Math.round(imageWidth * scaleFactor);
                float scaleHeight = Math.round(imageHeight * scaleFactor);

                boolean canScrollX = viewWidth < scaleWidth;
                boolean canScrollY = viewHeight < scaleHeight;

                if (canScrollX) {
                    float newX = (viewWidth / 2) - focusX;
                    if ((focusX + deltaX) > newX) {
                        deltaX = newX - focusX;
                    } else if ((focusX + deltaX) < (scaleWidth - newX)) {
                        deltaX = scaleWidth - newX - focusX;
                    }
                } else {
                    deltaX = 0;
                }

                if (canScrollY) {
                    float newY = (viewHeight / 2) - focusY;
                    if ((focusY + deltaY) > newY) {
                        deltaY = newY - focusY;
                    } else if ((focusY + deltaY) < (scaleHeight - newY)) {
                        deltaY = scaleHeight - newY - focusY;
                    }
                } else {
                    deltaY = 0;
                }

                matrix.postTranslate(deltaX, deltaY);
                setImageMatrix(matrix);
                break;
        }
        return true;
    }

    private class ScaleListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float previousScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(originalImageScale, scaleFactor);
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();

            if (previousScale != scaleFactor) {
                matrix.setScale(scaleFactor, scaleFactor, focusX, focusY);
                setImageMatrix(matrix);
            }

            return true;
        }
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        this.matrix.set(matrix);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        initializeImage();
    }

    private void initializeImage() {
        imageWidth = getDrawable().getIntrinsicWidth();
        imageHeight = getDrawable().getIntrinsicHeight();
        originalImageScale = 1.0f; // Set the original scale to 1.0 to prevent zooming out.
        matrix.reset();
        float scale = Math.min((float) viewWidth / imageWidth, (float) viewHeight / imageHeight);
        float dx = (viewWidth - imageWidth * scale) / 2;
        float dy = (viewHeight - imageHeight * scale) / 2;
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }
}

