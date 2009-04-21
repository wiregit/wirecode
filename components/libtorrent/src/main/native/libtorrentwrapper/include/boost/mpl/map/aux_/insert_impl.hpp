
#ifndef BOOST_MPL_MAP_AUX_INSERT_IMPL_HPP_INCLUDED
#define BOOST_MPL_MAP_AUX_INSERT_IMPL_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2003-2004
// Copyright David Abrahams 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: insert_impl.hpp,v 1.1.2.1 2009-04-21 21:43:43 pvertenten Exp $
// $Date: 2009-04-21 21:43:43 $
// $Revision: 1.1.2.1 $

#include <boost/mpl/insert_fwd.hpp>
#include <boost/mpl/next_prior.hpp>
#include <boost/mpl/map/aux_/contains_impl.hpp>
#include <boost/mpl/map/aux_/item.hpp>
#include <boost/mpl/map/aux_/tag.hpp>
#include <boost/mpl/aux_/na.hpp>
#include <boost/mpl/aux_/config/typeof.hpp>

namespace boost { namespace mpl {

namespace aux {
template< typename Map, typename Pair > 
struct map_insert_impl
    : if_< 
          contains_impl<aux::map_tag>::apply<Map,Pair>
        , Map
#if defined(BOOST_MPL_CFG_TYPEOF_BASED_SEQUENCES)
        , m_item<
              typename Pair::first
            , typename Pair::second
            , Map
            >
#else
        , m_item<
              next< typename Map::size >::type::value
            , typename Pair::first
            , typename Pair::second
            , Map
            >
#endif
        >
{
};
}

template<>
struct insert_impl< aux::map_tag >
{
    template< 
          typename Map
        , typename PosOrKey
        , typename KeyOrNA
        > 
    struct apply
        : aux::map_insert_impl<
              Map
            , typename if_na<KeyOrNA,PosOrKey>::type
            >
    {
    };
};

}}

#endif // BOOST_MPL_MAP_AUX_INSERT_IMPL_HPP_INCLUDED
