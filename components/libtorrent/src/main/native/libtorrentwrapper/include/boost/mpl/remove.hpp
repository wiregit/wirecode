
#ifndef BOOST_MPL_REMOVE_HPP_INCLUDED
#define BOOST_MPL_REMOVE_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2000-2004
// Copyright David Abrahams 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: remove.hpp,v 1.2 2009-05-21 17:03:42 pvertenten Exp $
// $Date: 2009-05-21 17:03:42 $
// $Revision: 1.2 $

#include <boost/mpl/remove_if.hpp>
#include <boost/mpl/same_as.hpp>
#include <boost/mpl/aux_/inserter_algorithm.hpp>

namespace boost { namespace mpl {

namespace aux {

template<
      typename Sequence
    , typename T
    , typename Inserter 
    >
struct remove_impl
    : remove_if_impl< Sequence, same_as<T>, Inserter >
{
};

template<
      typename Sequence
    , typename T
    , typename Inserter 
    >
struct reverse_remove_impl
    : reverse_remove_if_impl< Sequence, same_as<T>, Inserter >
{
};

} // namespace aux

BOOST_MPL_AUX_INSERTER_ALGORITHM_DEF(3, remove)

}}

#endif // BOOST_MPL_REMOVE_HPP_INCLUDED
