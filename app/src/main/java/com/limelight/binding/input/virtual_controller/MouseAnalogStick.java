package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.content.Context;

import com.limelight.binding.input.evdev.EvdevListener;

/**
 * A velocity-based mouse stick. Holding the stick away from center continuously moves the cursor.
 */
@SuppressLint("ViewConstructor")
public class MouseAnalogStick extends AnalogStick {
    private static final long FRAME_DELAY_MS = 16;
    private static final float MAX_PIXELS_PER_FRAME = 28f;

    private final EvdevListener inputSink;
    private float velocityX;
    private float velocityY;
    private boolean running;

    private final Runnable mouseMoveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            int deltaX = Math.round(velocityX * MAX_PIXELS_PER_FRAME);
            int deltaY = Math.round(-velocityY * MAX_PIXELS_PER_FRAME);
            if (inputSink != null && (deltaX != 0 || deltaY != 0)) {
                inputSink.mouseMove(deltaX, deltaY);
            }
            virtualController.getHandler().postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public MouseAnalogStick(VirtualController controller, Context context, int elementId) {
        super(controller, context, elementId);
        inputSink = controller.getInputSink();

        addAnalogStickListener(new AnalogStickListener() {
            @Override
            public void onMovement(float x, float y) {
                velocityX = x;
                velocityY = y;
                if (!running && (x != 0f || y != 0f)) {
                    running = true;
                    virtualController.getHandler().post(mouseMoveRunnable);
                }
            }

            @Override public void onClick() { }
            @Override public void onDoubleClick() { }

            @Override
            public void onRevoke() {
                releaseInput();
            }
        });
    }

    @Override
    public void releaseInput() {
        running = false;
        velocityX = velocityY = 0f;
        virtualController.getHandler().removeCallbacks(mouseMoveRunnable);
    }
}
