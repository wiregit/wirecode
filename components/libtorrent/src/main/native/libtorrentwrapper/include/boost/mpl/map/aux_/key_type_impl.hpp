
#ifndef BOOST_MPL_MAP_AUX_KEY_TYPE_IMPL_HPP_INCLUDED
#define BOOST_MPL_MAP_AUX_KEY_TYPE_IMPL_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2003-2004
// Copyright David Abrahams 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: key_type_impl.hpp,v 1.2 2009-05-21 17:03:44 pvertenten Exp $
// $Date: 2009-05-21 17:03:44 $
// $Revision: 1.2 $

#include <boost/mpl/key_type_fwd.hpp>
#include <boost/mpl/pair.hpp>
#include <boost/mpl/map/aux_/tag.hpp>

namespace boost {
namespace mpl {

template<>
struct key_type_impl< aux::map_tag >
{
    template< typename Map, typename T > struct apply
        : first<T>
    {
    };
};

}}

#endif // BOOST_MPL_MAP_AUX_KEY_TYPE_IMPL_HPP_INCLUDED
