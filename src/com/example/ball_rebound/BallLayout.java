package com.example.ball_rebound;

import java.util.ArrayList;
import java.util.List;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringSystemListener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class BallLayout extends FrameLayout implements SpringListener,
		SpringSystemListener {

	private Paint mPaint;
	private float radius = 100f;
	private float drag_ball_radius = radius - 10f;
	private float mCenterwidth;
	private float mCenterlheight;
	private float x;
	private float y;
	private float mdownX;
	private float mdownY;
	private float mlastX;
	private float mlastY;
	private float attractionThreshold = 150;
	private boolean draging = false;
	private List<PointF> points = new ArrayList<>();
	private Spring mreboundSpringY;
	private Spring mreboundSpringX;
	private SpringSystem mSpringSystem;
	private SpringConfig mSpringConfig = new SpringConfig(80, 10);
	private final SpringConfig COASTING = SpringConfig
			.fromOrigamiTensionAndFriction(0, 0.9);;
	private VelocityTracker mVelocityTracker;

	public BallLayout(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public BallLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub

	}

	public BallLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setBackgroundColor(Color.RED);
		// setWillNotDraw(false);
		// TODO Auto-generated constructor stub
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		mSpringSystem = SpringSystem.create();
		mSpringSystem.addListener(this);
		mreboundSpringX = mSpringSystem.createSpring();
		mreboundSpringY = mSpringSystem.createSpring();
		mreboundSpringX.setSpringConfig(mSpringConfig);
		mreboundSpringX.addListener(this);

		mreboundSpringY.setSpringConfig(mSpringConfig);
		mreboundSpringY.addListener(this);

		// 在整个布局没有加载出来时计算宽高是的不出结果的（得到的也是0）。所以必须在此事件监听函数中求宽高
		getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						mCenterwidth = (float) (getWidth() / 2.0f);
						mCenterlheight = (float) (getHeight() / 2.0f);
						mreboundSpringX.setCurrentValue(mCenterwidth)
								.setAtRest();
						mreboundSpringY.setCurrentValue(mCenterlheight)
								.setAtRest();
						getViewTreeObserver()
								.removeOnGlobalLayoutListener(this);
						for (float x = 0.0f; x <= getWidth(); x += getWidth()) {
							for (float y = 0.0f; y <= getHeight(); y += mCenterlheight) {
								points.add(new PointF(x, y));
								System.out.println("X:" + x + "    y:" + y);
							}
						}
					}
				});
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		// 先画6个洞
		System.out.println("onDraw" + points.size());
		int bg = Color.rgb(200, 189, 56);
		canvas.drawColor(bg);
		for (PointF point : points) {
			mPaint.setColor(Color.rgb(12, 250, 70));
			mPaint.setStyle(Paint.Style.FILL);
			canvas.drawCircle(point.x, point.y, radius, mPaint);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setColor(Color.rgb(255, 255, 255));
			mPaint.setStrokeWidth(15);

			canvas.drawCircle(point.x, point.y, radius, mPaint);
		}

		mPaint.setColor(Color.rgb(0, 12, 250));
		mPaint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(x, y, drag_ball_radius, mPaint);
		mPaint.setColor(Color.rgb(255, 255, 255));
		mPaint.setTextSize(36);
		mPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText("Drag", x, y + 10, mPaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		float touchX = event.getRawX();
		float touchY = event.getRawY();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mdownX = touchX;
			mdownY = touchY;
			mlastX = mdownX;
			mlastY = mdownY;
			mVelocityTracker = VelocityTracker.obtain();
			mVelocityTracker.addMovement(event);
			if (mdownX >= x - drag_ball_radius
					&& mdownX <= x + drag_ball_radius
					&& mdownY >= y - drag_ball_radius
					&& mdownY <= y + drag_ball_radius) {
				draging = true;
			}

		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (draging) {
				mVelocityTracker.addMovement(event);
				float offsetX = mlastX - touchX;
				float offsetY = mlastY - touchY;
				mreboundSpringX.setCurrentValue(
						mreboundSpringX.getCurrentValue() - offsetX)
						.setAtRest();
				mreboundSpringY.setCurrentValue(
						mreboundSpringY.getCurrentValue() - offsetY)
						.setAtRest();
				checkBoundary();
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP
				|| event.getAction() == MotionEvent.ACTION_CANCEL) {

			if (draging) {
				draging = false;
				mVelocityTracker.addMovement(event);
				mVelocityTracker.computeCurrentVelocity(1000);
				mreboundSpringX.setSpringConfig(COASTING);
				mreboundSpringY.setSpringConfig(COASTING);
				mreboundSpringX.setVelocity(mVelocityTracker.getXVelocity());
				mreboundSpringY.setVelocity(mVelocityTracker.getYVelocity());
			}
		}
		mlastX = touchX;
		mlastY = touchY;
		return true;
	}

	private void checkBoundary() {
		if ((x - drag_ball_radius < 0)
				&& ((y > getHeight() / 2 + drag_ball_radius && y < getHeight()
						- drag_ball_radius) || (y > drag_ball_radius && y < getHeight()
						/ 2 - drag_ball_radius))) {
			mreboundSpringX.setVelocity(-mreboundSpringX.getVelocity());
			mreboundSpringX.setCurrentValue(mreboundSpringX.getCurrentValue()
					- (x - drag_ball_radius), false);
		}

		if ((x + drag_ball_radius >= getWidth())
				&& ((y > getHeight() / 2 + drag_ball_radius && y < getHeight()
						- drag_ball_radius)
				|| (y > drag_ball_radius && y < getHeight() / 2
						- drag_ball_radius))) {

			mreboundSpringX.setVelocity(-mreboundSpringX.getVelocity());
			mreboundSpringX.setCurrentValue(mreboundSpringX.getCurrentValue()
					- (x + drag_ball_radius - getWidth()), false);
		}
		if (y - drag_ball_radius < 0
				&& ((x > drag_ball_radius) && (x < getWidth()
						- drag_ball_radius))) {
			mreboundSpringY.setVelocity(-mreboundSpringY.getVelocity());
			mreboundSpringY.setCurrentValue(mreboundSpringY.getCurrentValue()
					- (y - drag_ball_radius), false);

		}
		if (y + drag_ball_radius >= getHeight()
				&& ((x >= drag_ball_radius) && (x <= getWidth()
						- drag_ball_radius))) {
			mreboundSpringY.setVelocity(-mreboundSpringY.getVelocity());
			mreboundSpringY.setCurrentValue(mreboundSpringY.getCurrentValue()
					- (y + drag_ball_radius - getHeight()), false);
		}

		// if (x <= drag_ball_radius && y <= drag_ball_radius) {
		// mreboundSpringY.setVelocity(900);
		// mreboundSpringX.setVelocity(900);
		// }
		for (PointF point : points) {
			if (dist(x, y, point.x, point.y) < attractionThreshold
					&& Math.abs(mreboundSpringX.getVelocity()) < 1500
					&& Math.abs(mreboundSpringY.getVelocity()) < 1500
					&& !draging) {
				mreboundSpringX.setSpringConfig(mSpringConfig);
				mreboundSpringX.setEndValue(point.x);
				mreboundSpringY.setSpringConfig(mSpringConfig);
				mreboundSpringY.setEndValue(point.y);
			}
		}
	}

	@Override
	public void onAfterIntegrate(BaseSpringSystem arg0) {
		// TODO Auto-generated method stub
		checkBoundary();
	}

	@Override
	public void onBeforeIntegrate(BaseSpringSystem arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpringActivate(Spring arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpringAtRest(Spring arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpringEndStateChange(Spring arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpringUpdate(Spring spring) {
		// TODO Auto-generated method stub
		x = (float) mreboundSpringX.getCurrentValue();
		y = (float) mreboundSpringY.getCurrentValue();
		System.out.println("onSpringUpdate is invoke");
		invalidate();
	}

	private float dist(double posX, double posY, double pos2X, double pos2Y) {
		return (float) Math.sqrt(Math.pow(pos2X - posX, 2)
				+ Math.pow(pos2Y - posY, 2));
	}
}
