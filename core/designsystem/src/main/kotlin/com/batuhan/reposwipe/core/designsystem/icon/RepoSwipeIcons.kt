package com.batuhan.reposwipe.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.batuhan.reposwipe.core.designsystem.R

/** Central mapping from the mockups' Material Symbols glyph names to Compose Material Icons. */
object RepoSwipeIcons {
    val Menu = Icons.Outlined.Menu
    val Back = Icons.AutoMirrored.Outlined.ArrowBack

    // Custom brand glyph (see res/drawable/ic_filter.xml) rather than the stock Icons.Outlined.Tune
    // — resource-backed vectors need a Composable context to resolve, unlike the plain Icons.*
    // constants above, so this is the one entry in this object that's a @Composable getter.
    val Filters: ImageVector
        @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_filter)
    val Search = Icons.Outlined.Search
    val Discover = Icons.Outlined.Explore
    val DiscoverFilled = Icons.Filled.Explore
    val Trending = Icons.AutoMirrored.Outlined.TrendingUp
    val TrendingFilled = Icons.AutoMirrored.Filled.TrendingUp
    val Star = Icons.Outlined.Grade
    val StarFilled = Icons.Filled.Grade
    val Profile = Icons.Outlined.Person
    val ProfileFilled = Icons.Filled.Person
    val Fork = Icons.Outlined.CallSplit
    val UpdatedAt = Icons.Outlined.Schedule
    val Rewind = Icons.Outlined.Replay
    val Skip = Icons.Outlined.Close
    val Like = Icons.Outlined.Favorite
    val LikeOutline = Icons.Outlined.FavoriteBorder
    val QuickView = Icons.Outlined.Visibility
    val Info = Icons.Outlined.Info
    val RepoPlaceholder = Icons.Outlined.Terminal
    val RadioUnchecked = Icons.Outlined.RadioButtonUnchecked
    val ChevronRight = Icons.Outlined.ChevronRight
    val Add = Icons.Outlined.Add
    val Apply = Icons.Outlined.CheckCircle
    val OpenExternal = Icons.AutoMirrored.Outlined.OpenInNew
    val Settings = Icons.Outlined.Settings
    val Repo = Icons.Outlined.Book
    val SignOut = Icons.AutoMirrored.Outlined.Logout
    val Error = Icons.Outlined.ErrorOutline
    val NoResults = Icons.Outlined.SearchOff
    val RateLimited = Icons.Outlined.HourglassBottom
    val Leaderboard = Icons.Outlined.Leaderboard
    val Close = Icons.Outlined.Close
    val ApplyFilled = Icons.Filled.CheckCircle
    val Share = Icons.Outlined.Share
    val Offline = Icons.Outlined.WifiOff
    val SwipeLike = Icons.Filled.Favorite
    val SwipeReject = Icons.Filled.Delete
    val Copy = Icons.Outlined.ContentCopy
    val FollowAdd = Icons.Outlined.PersonAdd
    val FollowRemove = Icons.Outlined.PersonRemove
}
