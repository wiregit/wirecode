
#ifndef BOOST_MPL_MAP_AUX_EMPTY_IMPL_HPP_INCLUDED
#define BOOST_MPL_MAP_AUX_EMPTY_IMPL_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: empty_impl.hpp,v 1.2 2009-05-21 17:03:44 pvertenten Exp $
// $Date: 2009-05-21 17:03:44 $
// $Revision: 1.2 $

#include <boost/mpl/empty_fwd.hpp>
#include <boost/mpl/not.hpp>
#include <boost/mpl/map/aux_/tag.hpp>

namespace boost { namespace mpl {

template<>
struct empty_impl< aux::map_tag >
{
    template< typename Map > struct apply
        : not_< typename Map::size >
    {
    };
};

}}

#endif // BOOST_MPL_MAP_AUX_EMPTY_IMPL_HPP_INCLUDED
