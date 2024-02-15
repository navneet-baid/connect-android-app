package `in`.app.connect.bottomnav.Blog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import `in`.app.connect.R


class BottomBlogFragment : Fragment() {
    private lateinit var pagerAdapter: BlogsPagerAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_blog, container, false)

        viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        pagerAdapter = BlogsPagerAdapter(childFragmentManager, lifecycle)
        viewPager.adapter = pagerAdapter
        tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val tabs = arrayOf("For You", "Following")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        return view
    }
}

class BlogsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ForYouFragment()
            1 -> FollowingFragment()
            else -> ForYouFragment()
        }
    }
}
