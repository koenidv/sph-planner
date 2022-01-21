package de.koenidv.sph.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.koenidv.sph.ui.*

private const val NUM_PAGES = 4

//                              Ref. to the host          Host info is passed to super
//                              + MainActivity
class FragmentSlidePagerAdapter(fa: FragmentActivity) :   FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return HomeFragment()
            1 -> return CourseOverviewFragment()
            2 -> return ChatFragment()
            else -> return ExploreFragment()
        }
    }

    //fun getItem(position: Int): Fragment = ScreenSlidePageFragment()

}