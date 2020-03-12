package bogomolov.aa.anochat.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import bogomolov.aa.anochat.R


class RoundedImageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs) {

    init {
        ConstraintLayout.inflate(context, R.layout.rounded_image_layout, this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, 0, 0)

        val tintColor = a.getColor(R.styleable.RoundedImageView_foregroundColor, Color.BLACK)
        val roundFg: ImageView = findViewById(R.id.round_fg_image)
        roundFg.setColorFilter(tintColor)

        val drawableResId = a.getResourceId(R.styleable.RoundedImageView_src, -1);
        val drawable = AppCompatResources.getDrawable(context, drawableResId)
        val imageView: ImageView = findViewById(R.id.round_image)
        imageView.setImageDrawable(drawable)

        a.recycle()
    }

    fun setBitmap(bitmap: Bitmap) {
        val imageView: ImageView = findViewById(R.id.round_image)
        imageView.setImageBitmap(bitmap)
    }

}