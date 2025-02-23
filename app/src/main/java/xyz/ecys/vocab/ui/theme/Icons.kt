package xyz.ecys.vocab.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import xyz.ecys.vocab.R

object AppIcons {
    @Composable
    fun bookmarkOutline(): Painter {
        return painterResource(id = R.drawable.bookmark_regular)
    }

    @Composable
    fun bookmarkSolid(): Painter {
        return painterResource(id = R.drawable.bookmark_solid)
    }

    @Composable
    fun arrowLeft(): Painter {
        return painterResource(id = R.drawable.arrow_left)
    }

    @Composable
    fun boltSolid(): Painter {
        return painterResource(id = R.drawable.bolt_solid)
    }

    @Composable
    fun heartSolid(): Painter {
        return painterResource(id = R.drawable.heart_solid)
    }

    @Composable
    fun heartCrackSolid(): Painter {
        return painterResource(id = R.drawable.heart_crack_solid)
    }

    @Composable
    fun repeatSolid(): Painter {
        return painterResource(id = R.drawable.repeat_solid)
    }

    @Composable
    fun circleInfoSolid(): Painter {
        return painterResource(id = R.drawable.circle_info_solid)
    }

    @Composable
    fun cogSolid(): Painter {
        return painterResource(id = R.drawable.cog_solid)
    }

    @Composable
    fun chartSimpleSolid(): Painter {
        return painterResource(id = R.drawable.chart_simple_solid)
    }

    @Composable
    fun stopwatchSolid(): Painter {
        return painterResource(id = R.drawable.stopwatch_solid)
    }

    @Composable
    fun medalSolid(): Painter {
        return painterResource(id = R.drawable.medal_solid)
    }

    @Composable
    fun fireSolid(): Painter {
        return painterResource(id = R.drawable.fire_solid)
    }

    @Composable
    fun bellSolid(): Painter {
        return painterResource(id = R.drawable.bell_solid)
    }

    @Composable
    fun magnifyingGlass(): Painter {
        return painterResource(id = R.drawable.magnifying_glass)
    }

    @Composable
    fun xSolid(): Painter {
        return painterResource(id = R.drawable.x_solid)
    }

    @Composable
    fun bugSolid(): Painter {
        return painterResource(id = R.drawable.bug_solid)
    }

    @Composable
    fun sparklesSolid(): Painter {
        return painterResource(id = R.drawable.sparkles_solid)
    }
} 